import java.util.logging.Level;
import java.util.logging.Logger;


public class SomeSymbolSynchTest  implements Runnable{
    private char s;
    private SyncInt sync; // спільний обʼєкт
    private int controlValue;
    private int maxControl;

    public SomeSymbolSynchTest(char symbol, int maxControl, int control) {
        s = symbol;
        this.sync = sync;
        this.controlValue = control;
        this.maxControl = maxControl;
    }

    @Override
    public void run() {

        while (!sync.isStop()) {
            sync.waitAndChange(controlValue, maxControl, s);
        }


    }
}
