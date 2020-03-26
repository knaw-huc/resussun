package nl.knaw.huc.resussun.dataextension;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class GraphQlHelperText {
  @Test
  void replacesStartingNumberWithAnUnderscore() {
    assertThat(GraphQlHelper.escapeGraphQl("123test"), is("_23test"));
  }

  @Test
  void replacesEveryNonWordCharacterWithAnUnderscore() {
    assertThat(GraphQlHelper.escapeGraphQl("http://e#ample.org?-test"), is("http___e_ample_org__test"));
  }

}
