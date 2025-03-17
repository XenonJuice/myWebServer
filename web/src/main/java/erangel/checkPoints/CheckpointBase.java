package erangel.checkPoints;

import erangel.base.Checkpoint;
import erangel.base.CheckPointContext;
import erangel.base.Vas;
import erangel.base.VasManager;
import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;

public abstract class CheckpointBase implements Checkpoint,VasManager {
    //<editor-fold desc = "attr">
    protected Vas vas = null;
    //</editor-fold>
    //<editor-fold desc = "一次实现">
    @Override
    public Vas getVas() {
        return vas;
    }

    @Override
    public void setVas(Vas vas) {
        this.vas = vas;
    }

    public abstract void process(HttpRequest request, HttpResponse response, CheckPointContext context) throws Exception;
    //</editor-fold>
}
