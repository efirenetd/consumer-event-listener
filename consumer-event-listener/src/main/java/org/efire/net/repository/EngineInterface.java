package org.efire.net.repository;

public interface EngineInterface {

    default void go() {
        System.out.println("Go go go!!!");
    }

    void keyIn();
}
