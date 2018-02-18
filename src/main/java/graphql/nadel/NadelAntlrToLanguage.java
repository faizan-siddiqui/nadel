package graphql.nadel;

import graphql.language.Document;
import graphql.language.TypeDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.parser.GraphqlAntlrToLanguage;
import graphql.nadel.parser.antlr.StitchingDSLParser;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.parser.StringValueParsing.parseSingleQuotedString;

public class NadelAntlrToLanguage extends GraphqlAntlrToLanguage {

    private StitchingDsl stitchingDsl;


    public NadelAntlrToLanguage(CommonTokenStream tokens) {
        super(tokens);
    }

    enum NadelContextProperty {
        ServiceDefinition,

    }

    private static class NadelContextEntry {
        final NadelContextProperty contextProperty;
        final Object value;

        public NadelContextEntry(NadelContextProperty contextProperty, Object value) {
            this.contextProperty = contextProperty;
            this.value = value;
        }
    }

    private final Deque<NadelContextEntry> contextStack = new ArrayDeque<>();
    private final List<ContextEntry> contextEntriesRecorder = new ArrayList<>();

    private boolean recording;

    private void startRecording() {
        recording = true;
        contextEntriesRecorder.clear();
        ;
    }

    private void stopRecording() {
        recording = false;
    }

    @Override
    protected void addContextProperty(ContextProperty contextProperty, Object value) {
        super.addContextProperty(contextProperty, value);
        contextEntriesRecorder.add(getContextStack().getFirst());
    }

    protected void addContextProperty(NadelContextProperty contextProperty, Object value) {
        contextStack.addFirst(new NadelContextEntry(contextProperty, value));
    }

    private void popNadelContext() {
        contextStack.removeFirst();
    }

    protected Object getFromContextStack(NadelContextProperty contextProperty) {
        return getFromContextStack(contextProperty, false);
    }

    @SuppressWarnings("SameParameterValue")
    private Object getFromContextStack(NadelContextProperty contextProperty, boolean required) {
        for (NadelContextEntry contextEntry : contextStack) {
            if (contextEntry.contextProperty == contextProperty) {
                return contextEntry.value;
            }
        }
        if (required) {
            assertShouldNeverHappen("not found %s", contextProperty);
        }
        return null;
    }

    public StitchingDsl getStitchingDsl() {
        return stitchingDsl;
    }

    @Override
    public Void visitStitchingDSL(StitchingDSLParser.StitchingDSLContext ctx) {
        stitchingDsl = new StitchingDsl();
        setResult(new Document());
        return super.visitStitchingDSL(ctx);
    }

    @Override
    public Void visitServiceDefinition(StitchingDSLParser.ServiceDefinitionContext ctx) {
        String url = parseSingleQuotedString(ctx.serviceUrl().stringValue().getText());
        String name = ctx.name().getText();
        ServiceDefinition def = new ServiceDefinition(name, url);
        addContextProperty(NadelContextProperty.ServiceDefinition, def);
        super.visitChildren(ctx);
        popNadelContext();
        stitchingDsl.getServiceDefinitions().add(def);
        return null;
    }


    @Override
    public Void visitTypeDefinition(StitchingDSLParser.TypeDefinitionContext ctx) {
        startRecording();
        super.visitTypeDefinition(ctx);
        stopRecording();
        TypeDefinition typeDefinition = (TypeDefinition) contextEntriesRecorder.get(0).value;
        ServiceDefinition serviceDefinition = (ServiceDefinition) getFromContextStack(NadelContextProperty.ServiceDefinition);
        serviceDefinition.getTypeDefinitions().add(typeDefinition);
        return null;
    }
}
