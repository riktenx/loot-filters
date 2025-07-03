package com.lootfilters.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class CollectionUtil {
    private CollectionUtil() {}

    public static <E> List<E> append(List<E> list, E... elements) {
        var newList = new ArrayList<>(list);
        newList.addAll(Arrays.asList(elements));
        return newList;
    }

    public static <E> int[] findBounds(List<E> list, Predicate<E> predicate) { // [start, end)
        var indices = new int[]{-1, -1};
        for (var i = 0; i < list.size(); ++i) {
            if (indices[0] == -1 && predicate.test(list.get(i))) {
                indices[0] = i;
            }
            if (indices[0] != -1 && !predicate.test(list.get(i))) {
                indices[1] = i;
                return indices;
            }
        }
        if (indices[0] != -1) {
            indices[1] = list.size();
        }
        return indices;
    }
}
