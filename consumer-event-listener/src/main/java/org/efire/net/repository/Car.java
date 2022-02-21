package org.efire.net.repository;

public class Car implements EngineInterface {

    public void start() {
        EngineInterface.super.go();
        EngineInterface.super.go();
        EngineInterface.super.go();
        EngineInterface.super.go();
    }

    public static void main(String[] args) {
        Car car = new Car();
        car.keyIn();
        car.start();
    }

    @Override
    public void keyIn() {
        System.out.println("Put in key");
    }
}
