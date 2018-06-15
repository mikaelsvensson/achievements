package se.devscout.achievements.server;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class I18nTest {

    private I18n i18n;

    @Before
    public void setUp() throws Exception {
        i18n = new I18n("i18n-test.yaml");
    }

    @Test
    public void get_success() {
        assertThat(i18n.get("ancestor.name")).isEqualTo("Carol");
        assertThat(i18n.get("ancestor.parent.name")).isEqualTo("Bob");
        assertThat(i18n.get("ancestor.parent.child.name")).isEqualTo("Alice");
    }

    @Test
    public void get_missingNode() {
        assertThat(i18n.get("ancestor.ancient.name")).isNull();
    }

    @Test
    public void get_missingProperty() {
        assertThat(i18n.get("ancestor.unknown")).isNull();
    }

    @Test
    public void get_canOnlyReturnLeaf() {
        assertThat(i18n.get("ancestor")).isNull();
    }
}