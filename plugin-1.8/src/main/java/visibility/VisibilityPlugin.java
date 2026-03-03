package visibility;

import visibility.compat.Compat18;
import visibility.compat.VersionCompat;

/**
 * Plugin entry point for Minecraft 1.8.x.
 */
public class VisibilityPlugin extends VisibilityPluginBase {

    @Override
    protected VersionCompat createCompat() {
        return new Compat18(this);
    }
}
