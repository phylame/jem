package pw.phylame.seal;


import java.io.File;
import java.util.Comparator;

import lombok.val;
import pw.phylame.android.util.IOs;

class ItemSorter implements Comparator<File> {
    static final int BY_NAME = 1;
    static final int BY_TYPE = 2;
    static final int BY_DATE_ASC = 3;
    static final int BY_DATE_DESC = 4;
    static final int BY_SIZE_ASC = 5;
    static final int BY_SIZE_DESC = 6;

    /**
     * Make directory in front of file
     */
    boolean directoryFirst = true;

    /**
     * Make hidden files before others
     */
    int hiddenOrder = FRONTAL;

    /**
     * Sort type for files.
     */
    int sortType = BY_NAME;

    private static final int FRONTAL = -1;
    private static final int POSTERIOR = 1;

    @Override
    public int compare(File a, File b) {
        val dirA = a.isDirectory();
        val dirB = b.isDirectory();
        if (directoryFirst) {
            if (dirA) {
                if (!dirB) {
                    return FRONTAL;
                }
            } else if (dirB) {
                return POSTERIOR;
            }
        }

        val nameA = a.getName();
        val nameB = b.getName();

        if (dirA && dirB) { // directories only sorted by name
            return orderByName(nameA, nameB, FRONTAL);
        }

        switch (sortType) {
            case BY_NAME:
                return orderByName(nameA, nameB, hiddenOrder);
            case BY_TYPE:
                return orderByType(nameA, nameB, hiddenOrder);
            case BY_DATE_ASC:
                return orderByNumber(a.lastModified(), b.lastModified(), true, nameA, nameB, hiddenOrder);
            case BY_DATE_DESC:
                return orderByNumber(a.lastModified(), b.lastModified(), false, nameA, nameB, hiddenOrder);
            case BY_SIZE_ASC:
                return orderByNumber(a.length(), b.length(), true, nameA, nameB, hiddenOrder);
            case BY_SIZE_DESC:
                return orderByNumber(a.length(), b.length(), false, nameA, nameB, hiddenOrder);
        }
        return nameA.compareToIgnoreCase(nameB);
    }

    private int orderByHidden(String nameA, String nameB, int hiddenOrder) {
        if (hiddenOrder != 0) {
            if (IOs.isHidden(nameA)) {
                if (!IOs.isHidden(nameB)) {
                    return hiddenOrder;
                }
            } else if (IOs.isHidden(nameB)) {
                return -hiddenOrder;
            }
        }
        return 0;
    }

    private int orderByName(String nameA, String nameB, int hiddenOrder) {
        val order = orderByHidden(nameA, nameB, hiddenOrder);
        return order != 0 ? order : nameA.compareToIgnoreCase(nameB);
    }

    private int orderByType(String nameA, String nameB, int hiddenOrder) {
        int order = orderByHidden(nameA, nameB, hiddenOrder);
        if (order != 0) {
            return order;
        }
        order = IOs.extensionName(nameA).compareTo(IOs.extensionName(nameB));
        return order != 0 ? order : nameA.compareToIgnoreCase(nameB);
    }

    private int orderByNumber(long numA, long numB, boolean asc, String nameA, String nameB, int hiddenOrder) {
        int order = orderByHidden(nameA, nameB, hiddenOrder);
        if (order != 0) {
            return order;
        }
        if (numA == numB) {
            return nameA.compareToIgnoreCase(nameB);
        }
        order = numA < numB ? FRONTAL : POSTERIOR;
        return asc ? order : -order;
    }
}
