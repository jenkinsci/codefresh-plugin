/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codefresh;

import hudson.util.Secret;

/**
 *
 * @author antweiss
 */
public class CFComposition {
    private final String name;
    private final String id;

    public CFComposition(String name, String id ) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
    
}
