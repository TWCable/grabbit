package com.twc.webcms.sync.jcr

import org.apache.sling.api.resource.ValueMap;

/**
 * utilites for paths
 */
public class PathUtils {
    PathUtils() {}

    static String safeImageLookup(ValueMap properties, String path){
        String retVal = ""
        if(properties != null){
            retVal = properties.get(path,"")
        }
        retVal
    }

    static String contentPath(String path) {
        String jcrcontent = "jcr:content"
        int i = path.indexOf(jcrcontent);
        return (i >= 0) ? path.substring(0, i + jcrcontent.length()) : "";
    }

    static String stripContentPath(String path) {
        String contentPath = contentPath(path);
        return contentPath ? path.substring((contentPath + "/").length()) : path;
    }
}
