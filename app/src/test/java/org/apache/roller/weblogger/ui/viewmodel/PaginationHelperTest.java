package org.apache.roller.weblogger.ui.viewmodel;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginationHelperTest {
    
    @Test
    void testBasicPagination() {
        PaginationHelper helper = new PaginationHelper(10, 0, 100);
        
        assertEquals(10, helper.getTotalPages());
        assertEquals(0, helper.getStartIndex());
        assertEquals(10, helper.getEndIndex());
        assertFalse(helper.hasPreviousPage());
        assertTrue(helper.hasNextPage());
    }
    
    @Test
    void testLastPage() {
        PaginationHelper helper = new PaginationHelper(10, 9, 100);
        
        assertEquals(10, helper.getTotalPages());
        assertEquals(90, helper.getStartIndex());
        assertEquals(100, helper.getEndIndex());
        assertTrue(helper.hasPreviousPage());
        assertFalse(helper.hasNextPage());
    }
    
    @Test
    void testPartialLastPage() {
        PaginationHelper helper = new PaginationHelper(10, 2, 25);
        
        assertEquals(3, helper.getTotalPages());
        assertEquals(20, helper.getStartIndex());
        assertEquals(25, helper.getEndIndex());
    }
    
    @Test
    void testSliceForPage() {
        PaginationHelper helper = new PaginationHelper(5, 1, 12);
        List<String> items = Arrays.asList(
            "item0", "item1", "item2", "item3", "item4",
            "item5", "item6", "item7", "item8", "item9",
            "item10", "item11"
        );
        
        List<String> slice = helper.sliceForPage(items);
        
        assertEquals(5, slice.size());
        assertEquals("item5", slice.get(0));
        assertEquals("item9", slice.get(4));
    }
    
    @Test
    void testPageWindow() {
        PaginationHelper helper = new PaginationHelper(10, 5, 100);
        int[] window = helper.getPageWindow(5);
        
        assertEquals(5, window.length);
        assertEquals(3, window[0]);  // Current page 5, window starts at 3
        assertEquals(7, window[4]);  // Window ends at 7
    }
}