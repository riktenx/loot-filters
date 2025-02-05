package com.lootfilters.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectionUtil {
    private CollectionUtil() {}

    public static <E> List<E> append(List<E> list, E... elements) {
        var newList = new ArrayList<>(list);
        newList.addAll(Arrays.asList(elements));
        return newList;
    }

    public static <E> List<E> prepend(E element, List<E> list) {
        var newList = new ArrayList<E>();
        newList.add(element);
        newList.addAll(list);
        return newList;
    }

    public static <E> List<E> without(List<E> list, E element) {
        var newList = new ArrayList<>(list);
        newList.remove(element);
        return newList;
    }
}
