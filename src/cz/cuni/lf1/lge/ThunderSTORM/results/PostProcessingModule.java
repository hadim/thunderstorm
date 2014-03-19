package cz.cuni.lf1.lge.ThunderSTORM.results;

import cz.cuni.lf1.lge.ThunderSTORM.UI.GUI;
import cz.cuni.lf1.lge.ThunderSTORM.UI.MacroParser;
import cz.cuni.lf1.lge.thunderstorm.util.macroui.ParameterTracker;
import cz.cuni.lf1.lge.thunderstorm.util.macroui.validators.ValidatorException;
import ij.IJ;
import ij.Macro;
import ij.plugin.frame.Recorder;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public abstract class PostProcessingModule {

    protected JPanel uiPanel;
    protected ParameterTracker params = new ParameterTracker();

    ResultsTableWindow table;
    TripleStateTableModel model;

    public abstract String getMacroName();

    public abstract String getTabName();

    abstract protected JPanel createUIPanel();

    protected abstract void runImpl();

    public void run() {
        try {
            if(MacroParser.isRanFromMacro()) {
                params.readMacroOptions();
            } else {
                params.readDialogOptions();
            }

            runImpl();

            recordMacro();
        } catch(Exception e) {
            handleException(e);
        }
    }

    void setModel(TripleStateTableModel model) {
        this.model = model;
    }

    void setTable(ResultsTableWindow table) {
        this.table = table;
    }

    public JPanel getUIPanel() {
        if(uiPanel == null) {
            uiPanel = createUIPanel();
        }
        return uiPanel;
    }

    protected <T extends OperationsHistoryPanel.Operation> void saveStateForUndo(Class<T> cls) {
        final IJResultsTable rt = IJResultsTable.getResultsTable();
        final OperationsHistoryPanel history = rt.getOperationHistoryPanel();
        if(history.getLastOperation() != null
                && history.getLastOperation().getClass().equals(cls)) {
            if(!history.isLastOperationUndone()) {
                rt.swapUndoAndActual();     //undo last operation
            }
            history.removeLastOperation();
        }
        rt.copyActualToUndo();  //save state for later undo
    }

    protected void addOperationToHistory(OperationsHistoryPanel.Operation op) {
        final IJResultsTable rt = IJResultsTable.getResultsTable();
        final OperationsHistoryPanel history = rt.getOperationHistoryPanel();
        history.addOperation(op);
    }

    protected void handleException(Throwable ex) {
        if(ex instanceof ValidatorException) {
            ValidatorException vex = (ValidatorException) ex;
            Object source = vex.getSource();
            JComponent balloontipAnchor = (source != null && (source instanceof JComponent))
                    ? (JComponent) source
                    : uiPanel;
            if(balloontipAnchor != null) {
                GUI.showBalloonTip(balloontipAnchor, vex.getMessage());
            } else {
                IJ.handleException(ex);
            }
        } else {
            IJ.handleException(ex);
        }
    }

    public void recordMacro() {
        if(Recorder.record) {
            String oldCommand = Recorder.getCommand();
            Recorder.setCommand("Show results table");
            Recorder.recordOption("action", getMacroName());
            params.recordMacroOptions();
            Recorder.saveCommand();
            Recorder.setCommand(oldCommand);
        }
    }

    class DefaultOperation extends OperationsHistoryPanel.Operation {

        String options;

        public DefaultOperation() {
            //dummy record the param options
            boolean oldRecording = Recorder.record;
            Recorder.record = true;
            Recorder.setCommand("dummy");
            params.recordMacroOptions();
            options = Recorder.getCommandOptions();
            Recorder.record = oldRecording;
        }

        @Override
        protected String getName() {
            return getMacroName();
        }

        @Override
        protected void redo() {
            IJResultsTable rt = IJResultsTable.getResultsTable();
            rt.swapUndoAndActual();
            rt.setStatus(getTabName() + ": Redo.");
            rt.showPreview();
        }

        @Override
        protected void undo() {
            IJResultsTable rt = IJResultsTable.getResultsTable();
            rt.swapUndoAndActual();
            rt.setStatus(getTabName() + ": Undo.");
            rt.showPreview();
        }

        @Override
        protected void clicked() {
            if(uiPanel.getParent() instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) uiPanel.getParent();
                tabbedPane.setSelectedComponent(uiPanel);
            }
            //change thread name temporarily beacuse Macro.getOptions() requires the thread name to start with "Run$_"
            Thread thr = Thread.currentThread();
            String oldThreadName = thr.getName();
            thr.setName("Run$_workaround");
            //save old options values
            String oldOptions = Macro.getOptions();

            //set macro options and read param values
            Macro.setOptions(options);
            params.readMacroOptions();

            //restore options values and thread name
            Macro.setOptions(oldOptions);
            thr.setName(oldThreadName);

            params.updateComponents();
        }

        @Override
        protected boolean isUndoAble() {
            return true;
        }

    }
}
