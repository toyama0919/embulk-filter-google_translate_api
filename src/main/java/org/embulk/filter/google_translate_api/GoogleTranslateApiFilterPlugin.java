package org.embulk.filter.google_translate_api;

import java.util.List;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Types;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class GoogleTranslateApiFilterPlugin implements FilterPlugin
{
    public interface PluginTask extends Task
    {
        @Config("key_names")
        public List<String> getKeyNames();

        @Config("out_key_name_suffix")
        public String getOutKeyNameSuffix();

        @Config("source_lang")
        @ConfigDefault("null")
        public Optional<String> getSourceLang();

        @Config("target_lang")
        public String getTargetLang();

        @Config("google_api_key")
        @ConfigDefault("null")
        public Optional<String> getGoogleApiKey();

        @Config("sleep")
        @ConfigDefault("0")
        public Optional<Integer> getSleep();
    }

    @Override
    public void transaction(ConfigSource config, Schema inputSchema,
            FilterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        
        Schema outputSchema = buildOutputSchema(task, inputSchema);

        control.run(task.dump(), outputSchema);
    }

    @Override
    public PageOutput open(TaskSource taskSource, final Schema inputSchema, final Schema outputSchema, final PageOutput output)
    {
        return new GoogleTranslateApiPageOutput(taskSource, inputSchema, outputSchema, output);
    }

    /**
     * @param task
     * @param inputSchema
     * @return
     */
    private Schema buildOutputSchema(PluginTask task, Schema inputSchema) {
        ImmutableList.Builder<Column> builder = ImmutableList.builder();
        int i = 0;
        for (Column inputColumn: inputSchema.getColumns()) {
            Column outputColumn = new Column(i++, inputColumn.getName(), inputColumn.getType());
            builder.add(outputColumn);
        }
        for (String keyName : task.getKeyNames()) {
            builder.add(new Column(i++, keyName + task.getOutKeyNameSuffix(), Types.STRING));
        }
        Schema outputSchema = new Schema(builder.build());
        return outputSchema;
    }
}
