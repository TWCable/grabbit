/*
 * Copyright 2015 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twcable.grabbit.jcr

import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import com.twcable.grabbit.security.AuthorizablePrincipal
import com.twcable.grabbit.security.InsufficientGrabbitPrivilegeException
import com.twcable.grabbit.util.CryptoUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.ReflectPermission
import javax.annotation.Nonnull
import javax.jcr.Session
import org.apache.jackrabbit.api.security.user.Authorizable
import org.apache.jackrabbit.api.security.user.Group
import org.apache.jackrabbit.api.security.user.User
import org.apache.jackrabbit.api.security.user.UserManager
import org.apache.jackrabbit.value.StringValue
import org.apache.sling.jcr.base.util.AccessControlUtil

/**
 * This class wraps a serialized node that represents an Authorizable. Authorizables are special system protected nodes, that can only be written under certain
 * trees, and can not be written directly by a client.
 */
@CompileStatic
@Slf4j
class AuthorizableProtoNodeDecorator extends ProtoNodeDecorator {


    protected AuthorizableProtoNodeDecorator(@Nonnull ProtoNode node, @Nonnull Collection<ProtoPropertyDecorator> protoProperties) {
        this.innerProtoNode = node
        this.protoProperties = protoProperties
    }


    @Override
    protected JCRNodeDecorator writeNode(@Nonnull Session session) {
        if(!checkSecurityPermissions()) {
            throw new InsufficientGrabbitPrivilegeException("JVM Permissions needed by Grabbit to sync Users/Groups were not found. See log for specific permissions needed, and add these to your security manager; or do not sync users and groups." +
                                                            "Unfortunately, the way Jackrabbit goes about certain things requires us to do a bit of hacking in order to sync Authorizables securely, and efficiently.")
        }
        //the administrator is a special user that Jackrabbit will not let us mess with.
        if(getAuthorizableID() == 'admin') {
            return new JCRNodeDecorator(session.getNode(findAuthorizable(session, 'admin').getPath()))
        }

        Authorizable existingAuthorizable = findAuthorizable(session, getAuthorizableID())
        Authorizable newAuthorizable = existingAuthorizable ? updateAuthorizable(existingAuthorizable, session) : createNewAuthorizable(session)
        return new JCRNodeDecorator(session.getNode(newAuthorizable.getPath()), getID())
    }


    /**
     * @return a new authorizable from this serialized node
     */
    private Authorizable createNewAuthorizable(final Session session) {
        final UserManager userManager = getUserManager(session)
        if(isUserType()) {
            //We set a temporary password for now, and then set the real password later in setPasswordForUser(). See the method for why.
            final newUser = userManager.createUser(authorizableID, Long.toString(CryptoUtil.generateNextId()), new AuthorizablePrincipal(authorizableID), getParentPath())
            //This is a special protected property for disabling user access
            if(hasProperty('rep:disabled')) {
                newUser.disable(getStringValueFrom('rep:disabled'))
            }
            //AEM writes this property directly on the user node for some reason. One known use is for setting leads on MCM campaigns.
            final authorizableCategory = 'cq:authorizableCategory'
            if(hasProperty(authorizableCategory)) {
                newUser.setProperty(authorizableCategory, new StringValue(getStringValueFrom(authorizableCategory)))
            }
            //Special users may not have passwords, such as anonymous users
            if(hasProperty('rep:password')) {
                setPasswordForUser(newUser, session)
            }
            session.save()
            writeMandatoryPieces(session, newUser.getPath())
            return newUser
        }
        final Group newGroup = userManager.createGroup(authorizableID, new AuthorizablePrincipal(authorizableID), getParentPath())
        /*
         * Write all mandatory pieces, and find those that are authorizables. We then need to see if any of them have membership in this group, and add them.
         */
        final Collection<JCRNodeDecorator> authorizablePieces = writeMandatoryPieces(session, newGroup.getPath()).findAll { it.isAuthorizableType() }
        final Collection<JCRNodeDecorator> members = authorizablePieces.findAll { getMembershipIDs().contains(it.getTransferredID()) }
        members.each { JCRNodeDecorator member ->
            newGroup.addMember(member as Authorizable)
        }
        session.save()
        return newGroup
    }


    /**
     * From a client API perspective, there is really no way to truly update an existing authorizable node. All of the node properties are protected, and there is no
     * known way to update them. Here we remove the existing authorizable as denoted by the authorizableID, and recreate it.
     * @return new instance of updated authorizable
     */
    private Authorizable updateAuthorizable(final Authorizable authorizable, final Session session) {
        //We get all the declared groups of this authorizable so that we can add them back to the new, updated authorizable
        final Collection<Group> declaredGroups = authorizable.declaredMemberOf().toList()
        for(Group group : declaredGroups) {
            group.removeMember(authorizable)
        }
        authorizable.remove()
        session.save()
        final Authorizable newAuthorizable = createNewAuthorizable(session)
        for(Group group: declaredGroups) {
            group.addMember(newAuthorizable)
        }
        session.save()
        return newAuthorizable
    }


    private Authorizable findAuthorizable(final Session session, final String authorizableID) {
        final UserManager userManager = getUserManager(session)
        return userManager.getAuthorizable(authorizableID)
    }


    private String getAuthorizableID() {
        return protoProperties.find { it.isAuthorizableIDType() }.stringValue
    }


    private boolean isUserType() {
        return protoProperties.any { it.userType }
    }


    private Collection<String> getMembershipIDs() {
        return hasProperty('rep:members') ? getStringValuesFrom('rep:members') : []
    }


    /**
     * Some JVM's have a SecurityManager set, which based on configuration, can potentially inhibit our hack {@code setPasswordForUser(User, Session)} from working.
     * We need to check security permissions before proceeding
     * @return true if we can sync this Authorizable
     */
    private boolean checkSecurityPermissions() {
        final SecurityManager securityManager = getSecurityManager()
        //If no security manager is present, then we are in the clear; otherwise, we need to check certain permissions
        if(!securityManager){
            log.debug "No SecurityManager found on this JVM. Sync of Users/Groups can continue"
            return true
        }
        final issues = []
        final badPermissions = false
        log.debug "SecurityManager found on this JVM. Checking permissions.."
        try {
            //Needed to reflect on members for which this class does not normally have access to
            securityManager.checkPermission(new ReflectPermission('suppressAccessChecks'))
        }
        catch(SecurityException ex) {
            issues << 'suppressAccessChecks'
            badPermissions = true
        }
        try {
            //Needed to access all declared members of a class, including protected or private
            securityManager.checkPermission(new RuntimePermission('accessDeclaredMembers'))
        }
        catch(SecurityException ex) {
            issues << 'accessDeclaredMembers'
            badPermissions = true
        }
        try {
            //Needed to access classes directly within a potentially system protected package
            securityManager.checkPermission(new RuntimePermission('accessClassInPackage.{org.apache.jackrabbit.oak.security.user}'))
        }
        catch(SecurityException ex) {
            issues << 'accessClassInPackage.{org.apache.jackrabbit.oak.security.user}'
            badPermissions = true
        }
        if(badPermissions) {
            log.warn "A SecurityManager is enabled for this JVM, and permissions are not sufficient for Grabbit to sync Authorizables (Users/Groups). You must enable ${issues.join(', ')} permissions in your SecurityManager to use this functionality" +
                     "Check https://docs.oracle.com/javase/7/docs/api/java/lang/RuntimePermission.html and https://docs.oracle.com/javase/7/docs/api/java/lang/reflect/ReflectPermission.html to see what these permissions enable"
            return false
        }
        else {
            log.debug "Permissions check successful"
            return true
        }
    }

    /**
     * Mostly for ease of mocking/testing
     * @return the system's security manager, or null if one is not present
     */
    SecurityManager getSecurityManager() {
        return System.getSecurityManager()
    }

    /**
     * Mostly for ease of mocking/testing
     */
    UserManager getUserManager(final Session session) {
        return AccessControlUtil.getUserManager(session)
    }


    /**
     * Normally we would call org.apache.jackrabbit.oak.jcr.delegate.UserDelegator.changePassword(String password) to change a password (this is what is publicly available through the Jackrabbit API)
     * However, this method ALWAYS rehashes the password argument which is of no use to us, since we are trying to transfer an already hashed password.
     *
     * Internally, org.apache.jackrabbit.oak.jcr.delegate.UserDelegator calls it's delegate's org.apache.jackrabbit.oak.security.user.UserImpl.changePassword(String password)
     * which calls org.apache.jackrabbit.oak.security.user.UserManagerImpl.setPassword(Tree tree, String userId, String password, boolean forceHash) with forceHash always set to true
     * We really need forcehash set to false for our case, but this isn't publicly available. Here, we access internal objects to do this manipulation. org.apache.jackrabbit.oak.security.user.UserManagerImpl
     * simply ensures that forcehash is false, and that the password is not plain text, and it sets the password as-is.
     *
     * @throws IllegalStateException if security permissions required to run this are not there. @{code checkSecurityPermissions()} should be called before calling this method
     **/
     void setPasswordForUser(final User user, final Session session) {
        if(!checkSecurityPermissions()) throw new IllegalStateException("Security check failed for Grabbit. Can not set user passwords")
        //As a consumer we have access to org.apache.jackrabbit.oak.jcr.delegate.UserManagerDelegator below
        final userManager = getUserManager(session)
        Class userManagerDelegatorClass = userManager.getClass()
        //Reach into the class of this delegator, and grab the core Jackrabbit object we delegate to
        Field userManagerDelegateField = userManagerDelegatorClass.getDeclaredField('userManagerDelegate')
        //The delegate field is private, so we need to make it accessible. Security checks above are imperative for this to work
        userManagerDelegateField.setAccessible(true)
        //Here we have a handle to the internal class org.apache.jackrabbit.oak.security.user.UserManagerImpl
        final userManagerDelegate = userManagerDelegateField.get(userManager)
        final userManagerDelegateClass = userManagerDelegate.getClass()
        //We need to set the 'setPassword' method as accessible. Again, security checks above are imperative for this to work
        Method setPasswordMethod = userManagerDelegateClass.getDeclaredMethod('setPassword', Class.forName('org.apache.jackrabbit.oak.api.Tree', true, userManagerDelegateClass.getClassLoader()), String, String, boolean)
        setPasswordMethod.setAccessible(true)
        /**
        * Step two. We need access to the internal Authorizable object's tree in order to call the internal setPassword method
        * User is an instance of org.apache.jackrabbit.oak.jcr.delegate.UserDelegator. We need to get the delegate off of this class's super class org.apache.jackrabbit.oak.jcr.delegate.AuthorizableDelegator
        */
        Class authorizableDelegateClass = user.getClass().getSuperclass()
        Field authorizableDelegateField = authorizableDelegateClass.getDeclaredField('delegate')
        authorizableDelegateField.setAccessible(true)
        final authorizable = authorizableDelegateField.get(user)
        //Internal org.apache.jackrabbit.oak.security.user.AuthorizableImpl object. We can access the protected tree here
        Method getTreeMethod = authorizable.getClass().getSuperclass().getDeclaredMethod('getTree')
        getTreeMethod.setAccessible(true)

        /**
        * The last argument where we are passing in 'false' in the secret sauce we need. This parameter is forceHash. As long as forceHash is false, and the password is not
        * clear-text, which it isn't since we got it from another Jackrabbit instance, we can set the password as-is.
        */
        setPasswordMethod.invoke(userManagerDelegate, getTreeMethod.invoke(authorizable), getAuthorizableID(), getStringValueFrom('rep:password'), false)
    }
}
