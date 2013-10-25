package metridoc.cli;

import groovy.util.OptionAccessor;

/**
 * Created with IntelliJ IDEA on 10/25/13
 *
 * @author Tommy Barker
 */
public interface Command {
    boolean run(OptionAccessor optionAccessor);
    MetridocMain getMain();
}
