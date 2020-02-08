package com.example.damian.monitorapp.models;

public class MyDataGraph {
    public MyDataGraph(float x, int y) {
        this.valueX = x;
        this.valueY = y;
    }

    float valueX;
    int valueY;

    public float getValueX() {
        return valueX;
    }

    public int getValueY() {
        return valueY;
    }
}
