package hudson.plugins.pmd;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.analysis.core.BuildResult;
import hudson.plugins.analysis.core.FilesParser;
import hudson.plugins.analysis.core.HealthAwarePublisher;
import hudson.plugins.analysis.core.ParserResult;
import hudson.plugins.analysis.util.PluginLogger;
import hudson.plugins.pmd.parser.PmdParser;

/**
 * Publishes the results of the PMD analysis  (freestyle project type).
 *
 * @author Ulli Hafner
 */
public class PmdPublisher extends HealthAwarePublisher {
    private static final long serialVersionUID = 6711252664481150129L;

    private static final String PLUGIN_NAME = "PMD";

    /** Default PMD pattern. */
    private static final String DEFAULT_PATTERN = "**/pmd.xml";

    /** Ant file-set pattern of files to work with. */
    private String pattern = DEFAULT_PATTERN;

    /**
     * Creates a new instance of <code>PmdPublisher</code>.
     *
     * @param healthy
     *            Report health as 100% when the number of warnings is less than
     *            this value
     * @param unHealthy
     *            Report health as 0% when the number of warnings is greater
     *            than this value
     * @param thresholdLimit
     *            determines which warning priorities should be considered when
     *            evaluating the build stability and health
     * @param defaultEncoding
     *            the default encoding to be used when reading and parsing files
     * @param useDeltaValues
     *            determines whether the absolute annotations delta or the
     *            actual annotations set difference should be used to evaluate
     *            the build stability
     * @param unstableTotalAll
     *            annotation threshold
     * @param unstableTotalHigh
     *            annotation threshold
     * @param unstableTotalNormal
     *            annotation threshold
     * @param unstableTotalLow
     *            annotation threshold
     * @param unstableNewAll
     *            annotation threshold
     * @param unstableNewHigh
     *            annotation threshold
     * @param unstableNewNormal
     *            annotation threshold
     * @param unstableNewLow
     *            annotation threshold
     * @param failedTotalAll
     *            annotation threshold
     * @param failedTotalHigh
     *            annotation threshold
     * @param failedTotalNormal
     *            annotation threshold
     * @param failedTotalLow
     *            annotation threshold
     * @param failedNewAll
     *            annotation threshold
     * @param failedNewHigh
     *            annotation threshold
     * @param failedNewNormal
     *            annotation threshold
     * @param failedNewLow
     *            annotation threshold
     * @param canRunOnFailed
     *            determines whether the plug-in can run for failed builds, too
     * @param usePreviousBuildAsReference
     *            determines whether to always use the previous build as the reference build
     * @param useStableBuildAsReference
     *            determines whether only stable builds should be used as reference builds or not
     * @param canComputeNew
     *            determines whether new warnings should be computed (with
     *            respect to baseline)
     * @param shouldDetectModules
     *            determines whether module names should be derived from Maven POM or Ant build files
     * @param pattern
     *            Ant file-set pattern to scan for PMD files
     */
    // CHECKSTYLE:OFF
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public PmdPublisher(final String healthy, final String unHealthy, final String thresholdLimit,
            final String defaultEncoding, final boolean useDeltaValues,
            final String unstableTotalAll, final String unstableTotalHigh, final String unstableTotalNormal, final String unstableTotalLow,
            final String unstableNewAll, final String unstableNewHigh, final String unstableNewNormal, final String unstableNewLow,
            final String failedTotalAll, final String failedTotalHigh, final String failedTotalNormal, final String failedTotalLow,
            final String failedNewAll, final String failedNewHigh, final String failedNewNormal, final String failedNewLow,
            final boolean canRunOnFailed, final boolean usePreviousBuildAsReference, final boolean useStableBuildAsReference,
            final boolean shouldDetectModules, final boolean canComputeNew, final String pattern) {
        super(healthy, unHealthy, thresholdLimit, defaultEncoding, useDeltaValues,
                unstableTotalAll, unstableTotalHigh, unstableTotalNormal, unstableTotalLow,
                unstableNewAll, unstableNewHigh, unstableNewNormal, unstableNewLow,
                failedTotalAll, failedTotalHigh, failedTotalNormal, failedTotalLow,
                failedNewAll, failedNewHigh, failedNewNormal, failedNewLow,
                canRunOnFailed, usePreviousBuildAsReference, useStableBuildAsReference,
                shouldDetectModules, canComputeNew, false, PLUGIN_NAME);
        this.pattern = pattern;
    }

    // CHECKSTYLE:ON

    @DataBoundConstructor
    public PmdPublisher() {
        super(PLUGIN_NAME);
        setCanResolveRelativePaths(false);
    }

    @DataBoundSetter
    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    /**
     * Returns the Ant file-set pattern of files to work with.
     *
     * @return Ant file-set pattern of files to work with
     */
    public String getPattern() {
        return pattern;
    }

    @Override
    public Action getProjectAction(final AbstractProject<?, ?> project) {
        return new PmdProjectAction(project);
    }

    @Override
    public BuildResult perform(final Run<?, ?> build, final FilePath workspace, final TaskListener listener, final PluginLogger logger) throws InterruptedException, IOException {
        logger.log("Collecting PMD analysis files...");
        FilesParser pmdCollector = new FilesParser(PLUGIN_NAME, StringUtils.defaultIfEmpty(getPattern(), DEFAULT_PATTERN),
                new PmdParser(getDefaultEncoding()), shouldDetectModules(), isMavenBuild(build));
        ParserResult project = workspace.act(pmdCollector);
        logger.logLines(project.getLogMessages());

        PmdResult result = new PmdResult(build, getDefaultEncoding(), project,
                usePreviousBuildAsReference(), useOnlyStableBuildsAsReference());
        build.addAction(new PmdResultAction(build, this, result));

        return result;
    }

    @Override
    public PmdDescriptor getDescriptor() {
        return (PmdDescriptor)super.getDescriptor();
    }

    @Override
    public MatrixAggregator createAggregator(final MatrixBuild build, final Launcher launcher,
            final BuildListener listener) {
        return new PmdAnnotationsAggregator(build, launcher, listener, this,
                getDefaultEncoding(), usePreviousBuildAsReference(), useOnlyStableBuildsAsReference());
    }
}
