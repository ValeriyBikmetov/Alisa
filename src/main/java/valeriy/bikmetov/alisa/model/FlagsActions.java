package valeriy.bikmetov.alisa.model;

public class FlagsActions {
    boolean noCopy;
    boolean breakAct;

    public FlagsActions() {
        noCopy = false;
        breakAct = false;
    }

    public boolean isNoCopy() {return noCopy;}
    public boolean isBreakAct() {return breakAct;}

    public void setNoCopy(boolean value) {noCopy = value;}
    public void setBreakAct(boolean value) {breakAct = value;}

}
