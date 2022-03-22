package com.example.clockin.punch_sections;

import com.intrusoft.sectionedrecyclerview.Section;

import java.util.List;

public class SectionHeader implements Section<Child>, Comparable<SectionHeader> {

    /** Each section contains a list of child entries and the date they were all recorded on */

    List<Child> childList;
    String date;
    int index;

    public SectionHeader(List<Child> childList, String date, int index) {
        this.childList = childList;
        this.date = date;
        this.index = index;
    }

    @Override
    public List<Child> getChildItems() {
        return childList;
    }

    public String getSectionText() {
        return date;
    }

    @Override
    public int compareTo(SectionHeader another) {
        if (this.index > another.index) {
            return -1;
        } else {
            return 1;
        }
    }
}
