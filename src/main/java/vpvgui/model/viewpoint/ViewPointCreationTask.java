package vpvgui.model.viewpoint;

import javafx.concurrent.Task;

public abstract class ViewPointCreationTask extends Task<Void> {

    protected abstract Void call() throws Exception;
}
