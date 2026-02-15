package top.rymc.phira.main.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ServerArgs")
class ServerArgsTest {

    @Test
    @DisplayName("should use default values when no args")
    void shouldUseDefaultValuesWhenNoArgs() {
        var args = new ServerArgs(new String[]{});

        assertThat(args.getPort()).isEqualTo(12346);
        assertThat(args.getHost()).isEqualTo("0.0.0.0");
        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("plugins"));
        assertThat(args.isProxyProtocol()).isFalse();
    }

    @Test
    @DisplayName("should parse custom port")
    void shouldParseCustomPort() {
        var args = new ServerArgs(new String[]{"--port", "8080"});

        assertThat(args.getPort()).isEqualTo(8080);
    }

    @Test
    @DisplayName("should parse custom host")
    void shouldParseCustomHost() {
        var args = new ServerArgs(new String[]{"--host", "127.0.0.1"});

        assertThat(args.getHost()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("should parse custom plugins directory")
    void shouldParseCustomPluginsDirectory() {
        var args = new ServerArgs(new String[]{"--plugins", "custom-plugins"});

        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("custom-plugins"));
    }

    @Test
    @DisplayName("should parse proxy protocol flag")
    void shouldParseProxyProtocolFlag() {
        var args = new ServerArgs(new String[]{"--proxy-protocol", "true"});

        assertThat(args.isProxyProtocol()).isTrue();
    }

    @Test
    @DisplayName("should handle short flag format")
    void shouldHandleShortFlagFormat() {
        var args = new ServerArgs(new String[]{"-port", "9090"});

        assertThat(args.getPort()).isEqualTo(9090);
    }

    @Test
    @DisplayName("should parse multiple args")
    void shouldParseMultipleArgs() {
        var args = new ServerArgs(new String[]{
            "--port", "8080",
            "--host", "192.168.1.1",
            "--plugins", "./my-plugins"
        });

        assertThat(args.getPort()).isEqualTo(8080);
        assertThat(args.getHost()).isEqualTo("192.168.1.1");
        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("./my-plugins"));
    }
}
