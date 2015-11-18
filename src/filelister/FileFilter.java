/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filelister;

/**
 *
 * @author anthony.poon
 */
public interface FileFilter {
    //should make it into a event later
    public boolean shouldMove(FilterEvent evt);
    public String skipMessage(FilterEvent evt);
}
