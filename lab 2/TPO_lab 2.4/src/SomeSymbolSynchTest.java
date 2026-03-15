public class SomeSymbolSynchTest  implements Runnable{
    private final char s;
    private final SyncInt sync; // спільний обʼєкт
    private final int controlValue;
    private final int maxControl;

    public SomeSymbolSynchTest(char symbol, SyncInt sync, int maxControl, int control) {
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
