package com.twc.webcms.sync.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public final class JcrNodes {
    private JcrNodes() { }

    public static String getStringProperty(Node node, String property, String defaultValue) throws RepositoryException {
        return node.hasProperty(property) ? node.getProperty(property).getString() : defaultValue;
    }
    public static boolean getBooleanValue(Node contentNode, String property, boolean defaultValue) throws RepositoryException {
        return contentNode.hasProperty(property) ? contentNode.getProperty(property).getBoolean() : defaultValue;
    }
    public static String getDefaultAnalyticsName(Node node) throws RepositoryException {
        return node.getPath().substring(node.getPath().lastIndexOf("jcr:content")+12,node.getPath().length()).replace("/",":");
    }
}
