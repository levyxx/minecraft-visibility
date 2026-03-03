package visibility;

import visibility.compat.Compat112;
import visibility.compat.VersionCompat;

/**
 * Plugin entry point for Minecraft 1.12.x.
 */
public class VisibilityPlugin extends VisibilityPluginBase {

    @Override
    protected VersionCompat createCompat() {
        return new Compat112(this);
    }
}
