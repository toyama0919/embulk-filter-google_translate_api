package org.embulk.filter.google_translate_api;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.embulk.config.TaskSource;
import org.embulk.filter.google_translate_api.GoogleTranslateApiFilterPlugin.PluginTask;
import org.embulk.spi.Column;
import org.embulk.spi.Exec;
import org.embulk.spi.Page;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Types;
import org.slf4j.Logger;

import com.google.cloud.RetryParams;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.TranslateOptions.Builder;
import com.google.cloud.translate.Translation;
import com.google.common.collect.Lists;

public class GoogleTranslateApiPageOutput implements PageOutput
{
    private final PluginTask task;
    private final Schema outputSchema;
    private final List<Column> inputColumns;
    private final List<Column> keyNameColumns;
    private final PageReader reader;
    private final PageBuilder builder;
    private final Translate translate;
    private final TranslateOption srcLang;
    private final TranslateOption model;
    
    private static final Logger logger = Exec.getLogger(GoogleTranslateApiFilterPlugin.class);

    public GoogleTranslateApiPageOutput(TaskSource taskSource, Schema inputSchema, Schema outputSchema, PageOutput output) {
        this.task = taskSource.loadTask(PluginTask.class);
        this.outputSchema = outputSchema;
        this.inputColumns = inputSchema.getColumns();
        this.reader = new PageReader(inputSchema);
        this.builder = new PageBuilder(Exec.getBufferAllocator(), outputSchema, output);
        this.keyNameColumns = Lists.newArrayList();
        for (String keyName : task.getKeyNames()) {
            this.keyNameColumns.add(outputSchema.lookupColumn(keyName));
        }
        this.translate = createTranslateService();

        this.srcLang = task.getSourceLang().isPresent() ? TranslateOption.sourceLanguage(task.getSourceLang().get()) : null;
        this.model = task.getModel().isPresent() ? TranslateOption.model(task.getModel().get()) : null;
    }

    @Override
    public void finish() {
        builder.finish();
    }

    @Override
    public void close() {
        builder.close();
    }

    @Override
    public void add(Page page) {
        reader.setPage(page);

        try {
            while (reader.nextRecord()) {
                List<String> sourceTexts = Lists.newArrayList();
                for (Column keyNameColumn : keyNameColumns) {
                    String text = reader.isNull(keyNameColumn) ? "" : reader.getString(keyNameColumn);
                    logger.debug(text);
                    sourceTexts.add(text);
                }
                List<Translation> translations = translate(sourceTexts);
                for (int i = 0; i < keyNameColumns.size(); i++) {
                    builder.setString(outputSchema.lookupColumn(keyNameColumns.get(i).getName() + task.getOutKeyNameSuffix()), translations.get(i).getTranslatedText());
                }
                setValue(builder);
                builder.addRecord();
                Thread.sleep(task.getSleep().get());
            }
        } catch (Exception  e) {
            throw new RuntimeException(e);
        } 
    }

    /**
     * @param texts
     * @return
     */
    private List<Translation> translate(List<String> texts) {
        TranslateOption[] translateOptions = new TranslateOption[] {};
        if (srcLang != null) {
            translateOptions = ArrayUtils.add(translateOptions, srcLang);
        }
        if (model != null) {
            translateOptions = ArrayUtils.add(translateOptions, model);
        }
        return (translateOptions.length == 0) ? translate.translate(texts) : translate.translate(texts, translateOptions);
    }

    /**
     * @param builder
     */
    private void setValue(PageBuilder builder) {
        for (Column inputColumn: inputColumns) {
            if (reader.isNull(inputColumn)) {
                builder.setNull(inputColumn);
                continue;
            }
            if (Types.STRING.equals(inputColumn.getType())) {
                builder.setString(inputColumn, reader.getString(inputColumn));
            } else if (Types.BOOLEAN.equals(inputColumn.getType())) {
                builder.setBoolean(inputColumn, reader.getBoolean(inputColumn));
            } else if (Types.DOUBLE.equals(inputColumn.getType())) {
                builder.setDouble(inputColumn, reader.getDouble(inputColumn));
            } else if (Types.LONG.equals(inputColumn.getType())) {
                builder.setLong(inputColumn, reader.getLong(inputColumn));
            } else if (Types.TIMESTAMP.equals(inputColumn.getType())) {
                builder.setTimestamp(inputColumn, reader.getTimestamp(inputColumn));
            } else if (Types.JSON.equals(inputColumn.getType())) {
                builder.setJson(inputColumn, reader.getJson(inputColumn));
            }
        }
    }

    /**
     * @return
     */
    private Translate createTranslateService() {
        Builder builder = TranslateOptions.newBuilder()
            .setRetryParams(retryParams())
            .setConnectTimeout(60000)
            .setReadTimeout(60000)
            .setTargetLanguage(task.getTargetLang());

        if (task.getGoogleApiKey().isPresent()) {
            builder.setApiKey(task.getGoogleApiKey().get());
        }
        
        TranslateOptions translateOptions = builder.build();
        return translateOptions.getService();
      }

    /**
     * @return
     */
    private RetryParams retryParams() {
        return RetryParams.newBuilder()
            .setRetryMaxAttempts(3)
            .setMaxRetryDelayMillis(30000)
            .setTotalRetryPeriodMillis(120000)
            .setInitialRetryDelayMillis(250)
            .build();
      }
}
