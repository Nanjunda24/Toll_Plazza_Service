package com.tollplaza.util;

import com.tollplaza.model.GeoPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolylineDecoderTest {

    @Test
    void decodesReferencePolylineFromGoogleDocs() {
        List<GeoPoint> pts = PolylineDecoder.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@");

        assertThat(pts).hasSize(3);
        assertThat(pts.get(0).latitude()).isEqualTo(38.5);
        assertThat(pts.get(0).longitude()).isEqualTo(-120.2);
        assertThat(pts.get(1).latitude()).isEqualTo(40.7);
        assertThat(pts.get(1).longitude()).isEqualTo(-120.95);
        assertThat(pts.get(2).latitude()).isEqualTo(43.252);
        assertThat(pts.get(2).longitude()).isEqualTo(-126.453);
    }

    @Test
    void emptyOrNullGivesEmptyList() {
        assertThat(PolylineDecoder.decode("")).isEmpty();
        assertThat(PolylineDecoder.decode(null)).isEmpty();
    }

    @Test
    void truncatedInputIsRejected() {
        assertThatThrownBy(() -> PolylineDecoder.decode("_p~iF~ps|U_"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
