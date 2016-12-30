package pw.phylame.jem.epm.impl;

import static pw.phylame.jem.epm.util.VamUtils.openReader;

import java.io.File;
import java.io.IOException;

import pw.phylame.jem.epm.util.config.AbstractConfig;
import pw.phylame.jem.epm.util.config.Configured;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.vam.VamReader;

public abstract class VamParser<C extends VamParser.VamInConfig> extends AbstractParser<VamReader, C> {

    protected VamParser(String name, Class<C> clazz) {
        super(name, clazz);
    }

    @Override
    protected VamReader openInput(File file, C config) throws IOException {
        return StringUtils.isNotEmpty(config.inputType) ? openReader(file, config.inputType) : openReader(file);
    }

    public static class VamInConfig extends AbstractConfig {
        public static final String INPUT_TYPE = "vam.inputType";

        /**
         * Input type of the source file, may be 'dir', 'zip'.
         */
        @Configured(INPUT_TYPE)
        public String inputType;
    }
}
