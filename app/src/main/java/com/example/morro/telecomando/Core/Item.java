package com.example.morro.telecomando.Core;

import java.util.ArrayList;

/**
 * Created by morro on 15/02/18.
 */

/**
 * data model being displayed by the RecyclerView
 */

public class Item {
    private String itemPath;
    private String itemFolder;
    private String itemName;

    /*
    public Item(String name) {
        itemPath = name;
    }
*/

    public String getItemFolder() {
        return itemFolder;
    }

    public String getItemPath() {
        return itemPath;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName= itemName;
    }


    public Item(String itemPath) {
        this.itemPath = itemPath;
        itemName = itemPath.substring(itemPath.lastIndexOf("/")+1);
        itemFolder = itemPath.substring(0,itemPath.lastIndexOf("/"));
    }

    private static int countLines(String str){
        String[] lines = str.split("\r\n|\r|\n");
        return  lines.length;
    }

    public static ArrayList<Item> createTrackList(String content,ArrayList<Item> items) {
        String[] lines = content.split(System.getProperty("line.separator"));
        int nLines = lines.length;
        for(int i=1; i <= nLines ; i++){
            items.add(new Item(lines[0]));
        }

        return items;
    }

    public static ArrayList<Item> createTrackList(int numContacts) {
        ArrayList<Item> items = new ArrayList<Item>();

        for (int i = 1; i <= numContacts; i++) {
            items.add(new Item("Song Name "));
        }

        return items;
    }

}
