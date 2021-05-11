package de.tu_darmstadt.stg.sourcerule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class Example {

    private String str = "str";

    public void hello(ArrayList list, int index) {
        if(index < list.size()){
            list.get(index);
        }
    }

    public void method1(String str){
        return;
    }
}
