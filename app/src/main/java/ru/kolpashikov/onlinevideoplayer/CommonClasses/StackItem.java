package ru.kolpashikov.onlinevideoplayer.CommonClasses;

/**
 * Created by Denis on 13.02.2018.
 */

public class StackItem {
    public String urlToResource;
    public String resourceId;

    public StackItem(String urlToResource, String resourceId) {
        this.urlToResource = urlToResource;
        this.resourceId = resourceId;
    }
}
