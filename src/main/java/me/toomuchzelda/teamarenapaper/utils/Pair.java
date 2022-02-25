package me.toomuchzelda.teamarenapaper.utils;

import java.util.Map;

public class Pair<L, R> implements Map.Entry<L, R>{

    public L left;
    public R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public L getKey() {
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R r) {
        R temp = right;
        this.right = r;
        return temp;
    }
}
