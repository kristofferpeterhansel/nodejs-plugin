package jenkins.plugins.nodejs.tools;

import com.google.common.base.Throwables;
import hudson.*;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import jenkins.plugins.nodejs.NodeJSPlugin;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author fcamblor
 */
public class NpmPackagesBuildWrapper extends BuildWrapper {

    private String nodeJSInstallationName;

    @DataBoundConstructor
    public NpmPackagesBuildWrapper(String nodeJSInstallationName){
        this.nodeJSInstallationName = nodeJSInstallationName;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException {
        return new Environment(){
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                return true;
            }
        };
    }

    public String getNodeJSInstallationName() {
        return nodeJSInstallationName;
    }

    @Override
    public Launcher decorateLauncher(final AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {
        return new DecoratedLauncher(launcher){
            @Override
            public Proc launch(ProcStarter starter) throws IOException {
                // Avoiding potential NPE when calling starter.envs()
                // Yes, this is weird...
                String[] starterEnvs;
                try {
                   starterEnvs = starter.envs();
                } catch (NullPointerException ex) {
                    starterEnvs = new String[0];
                }


                EnvVars vars = toEnvVars(starterEnvs);

                NodeJSInstallation nodeJSInstallation = 
                    NodeJSPlugin.instance().findInstallationByName(nodeJSInstallationName);

                try {
                    nodeJSInstallation = nodeJSInstallation.forNode(build.getBuiltOn(), listener)
                                                           .forEnvironment(vars);
                } catch (InterruptedException e) {
                    Throwables.propagate(e);
                }

                vars.override("PATH+PATH", nodeJSInstallation.binFolder());

                return super.launch(starter.envs(Util.mapToEnv(vars)));
            }

            private EnvVars toEnvVars(String[] envs) {
                EnvVars vars = new EnvVars();
                for (String line : envs) {
                    vars.addLine(line);
                }
                return vars;
            }
        };
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(NpmPackagesBuildWrapper.class);
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        /**
         * @return available node js installations
         */
        public NodeJSInstallation[] getInstallations() {
            return NodeJSPlugin.instance().getInstallations();
        }

        public String getDisplayName() {
            return jenkins.plugins.nodejs.tools.Messages.NpmPackagesBuildWrapper_displayName();
        }
    }
}
