package edu.iastate.cs.egroum.aug;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import org.junit.Test;

import static de.tu_darmstadt.stg.mudetect.aug.matchers.AUGMatchers.*;
import static de.tu_darmstadt.stg.mudetect.aug.matchers.NodeMatchers.actionNodeWith;
import static de.tu_darmstadt.stg.mudetect.aug.matchers.NodePropertyMatchers.label;
import static edu.iastate.cs.egroum.aug.AUGBuilderTestUtils.buildAUG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class EncodeArithmeticTest {

    @Test
    public void notencodesArithmeticOperator() {
    	AUGConfiguration conf = new AUGConfiguration() {{ encodeArithmeticOperators = false; }};
        APIUsageExample aug = buildAUG(
                "void m(String s) { if (s.length() + s.indexOf(\"foo\") > 0) s.getBytes(); }",
                conf);

        if (conf.buildTransitiveDataEdges) {
            assertThat(aug, hasParameterEdge(actionNodeWith(label("String.length()")), actionNodeWith(label("<a>"))));
            assertThat(aug, hasParameterEdge(actionNodeWith(label("String.indexOf()")), actionNodeWith(label("<a>"))));
        }
        assertThat(aug, not(hasNode(actionNodeWith(label("<a>")))));
        assertThat(aug, hasSelectionEdge(actionNodeWith(label("CharSequence.length()")), actionNodeWith(label("String.getBytes()"))));
        assertThat(aug, hasSelectionEdge(actionNodeWith(label("String.indexOf()")), actionNodeWith(label("String.getBytes()"))));
    }

    @Test
    public void encodesArithmeticOperator() {
    	AUGConfiguration conf = new AUGConfiguration() {{ encodeArithmeticOperators = true; }};
        APIUsageExample aug = buildAUG(
                "void m(String s) { if (s.length() + s.indexOf(\"foo\") > 0) s.getBytes(); }",
                conf);

        if (conf.buildTransitiveDataEdges) {
            assertThat(aug, hasParameterEdge(actionNodeWith(label("String.length()")), actionNodeWith(label("<a>"))));
            assertThat(aug, hasParameterEdge(actionNodeWith(label("String.indexOf()")), actionNodeWith(label("<a>"))));
        }
        assertThat(aug, hasSelectionEdge(actionNodeWith(label("<a>")), actionNodeWith(label("String.getBytes()"))));
        assertThat(aug, hasSelectionEdge(actionNodeWith(label("CharSequence.length()")), actionNodeWith(label("String.getBytes()"))));
        assertThat(aug, hasSelectionEdge(actionNodeWith(label("String.indexOf()")), actionNodeWith(label("String.getBytes()"))));
    }
}
