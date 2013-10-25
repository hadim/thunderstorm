package cz.cuni.lf1.lge.ThunderSTORM.estimators.ui;

import cz.cuni.lf1.lge.ThunderSTORM.estimators.LSQFitter;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.IEstimator;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.MLEFitter;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.MultipleLocationsImageFitting;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.SymmetricGaussianPSF;
import cz.cuni.lf1.lge.ThunderSTORM.util.GridBagHelper;
import cz.cuni.lf1.lge.thunderstorm.util.macroui.ParameterName;
import cz.cuni.lf1.lge.thunderstorm.util.macroui.validators.DoubleValidatorFactory;
import cz.cuni.lf1.lge.thunderstorm.util.macroui.validators.IntegerValidatorFactory;
import cz.cuni.lf1.lge.thunderstorm.util.macroui.validators.StringValidatorFactory;
import java.awt.GridBagLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SymmetricGaussianEstimatorUI extends IEstimatorUI {

    protected String name = "PSF: Gaussian";
    protected CrowdedFieldEstimatorUI crowdedField;
    protected transient static final String MLE = "Maximum likelihood";
    protected transient static final String LSQ = "Least squares";
    //params
    protected transient ParameterName.Integer FITRAD;
    protected transient ParameterName.Choice METHOD;
    protected transient ParameterName.Double SIGMA;

    public SymmetricGaussianEstimatorUI() {
        crowdedField = new CrowdedFieldEstimatorUI();
        FITRAD = parameters.createIntField("fitradius", IntegerValidatorFactory.positiveNonZero(), 3);
        METHOD = parameters.createChoice("method", StringValidatorFactory.isMember(new String[]{MLE, LSQ}), LSQ);
        SIGMA = parameters.createDoubleField("sigma", DoubleValidatorFactory.positiveNonZero(), 1.6);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JPanel getOptionsPanel() {
        JTextField fitregsizeTextField = new JTextField("", 20);
        JComboBox<String> methodComboBox = new JComboBox<String>(new String[]{LSQ, MLE});
        JTextField sigmaTextField = new JTextField("");
        parameters.registerComponent(FITRAD, fitregsizeTextField);
        parameters.registerComponent(METHOD, methodComboBox);
        parameters.registerComponent(SIGMA, sigmaTextField);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel("Fitting radius [px]:"), GridBagHelper.leftCol());
        panel.add(fitregsizeTextField, GridBagHelper.rightCol());
        panel.add(new JLabel("Fitting method:"), GridBagHelper.leftCol());
        panel.add(methodComboBox, GridBagHelper.rightCol());
        panel.add(new JLabel("Initial sigma [px]:"), GridBagHelper.leftCol());
        panel.add(sigmaTextField, GridBagHelper.rightCol());
        crowdedField.getOptionsPanel(panel);

        parameters.loadPrefs();
        return panel;
    }

    @Override
    public void readParameters() {
        super.readParameters();
        crowdedField.readParameters();
    }

    @Override
    public IEstimator getImplementation() {
        String method = METHOD.getValue();
        double sigma = SIGMA.getValue();
        int fitradius = FITRAD.getValue();
        if(LSQ.equals(method)) {
            if(crowdedField.isEnabled()) {
                return crowdedField.getLSQImplementation(new SymmetricGaussianPSF(sigma), sigma, fitradius);
            } else {
                LSQFitter fitter = new LSQFitter(new SymmetricGaussianPSF(sigma));
                return new MultipleLocationsImageFitting(fitradius, fitter);
            }
        }
        if(MLE.equals(method)) {
            if(crowdedField.isEnabled()) {
                return crowdedField.getMLEImplementation(new SymmetricGaussianPSF(sigma), sigma, fitradius);
            } else {
                MLEFitter fitter = new MLEFitter(new SymmetricGaussianPSF(sigma));
                return new MultipleLocationsImageFitting(fitradius, fitter);
            }
        }
        //
        throw new IllegalArgumentException("Unknown fitting method: " + method);
    }

    @Override
    public void recordOptions() {
        super.recordOptions();
        crowdedField.recordOptions();
    }

    @Override
    public void readMacroOptions(String options) {
        super.readMacroOptions(options);
        crowdedField.readMacroOptions(options);
    }

    @Override
    public void resetToDefaults() {
        super.resetToDefaults();
        crowdedField.resetToDefaults();
    }
}
