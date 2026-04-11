package top.rymc.phira.main.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class ServerArgsTest {

    @Test
    @DisplayName("Should use default values when no arguments provided")
    void shouldUseDefaultValuesWhenNoArgumentsProvided() {
        ServerArgs args = new ServerArgs(new String[]{});

        assertThat(args.getPort()).isEqualTo(12346);
        assertThat(args.getHost()).isEqualTo("0.0.0.0");
        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("plugins"));
        assertThat(args.isProxyProtocol()).isFalse();
        assertThat(args.getDefaultLanguage()).isEqualTo("zh-CN");
    }

    @Test
    @DisplayName("Should parse custom port from arguments")
    void shouldParseCustomPortFromArguments() {
        ServerArgs args = new ServerArgs(new String[]{"--port", "8080"});

        assertThat(args.getPort()).isEqualTo(8080);
        assertThat(args.getHost()).isEqualTo("0.0.0.0");
        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("plugins"));
        assertThat(args.isProxyProtocol()).isFalse();
        assertThat(args.getDefaultLanguage()).isEqualTo("zh-CN");
    }

    @Test
    @DisplayName("Should parse custom host from arguments")
    void shouldParseCustomHostFromArguments() {
        ServerArgs args = new ServerArgs(new String[]{"--host", "127.0.0.1"});

        assertThat(args.getPort()).isEqualTo(12346);
        assertThat(args.getHost()).isEqualTo("127.0.0.1");
        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("plugins"));
        assertThat(args.isProxyProtocol()).isFalse();
        assertThat(args.getDefaultLanguage()).isEqualTo("zh-CN");
    }

    @Test
    @DisplayName("Should parse custom plugins directory from arguments")
    void shouldParseCustomPluginsDirectoryFromArguments() {
        ServerArgs args = new ServerArgs(new String[]{"--plugins", "/custom/plugins/path"});

        assertThat(args.getPort()).isEqualTo(12346);
        assertThat(args.getHost()).isEqualTo("0.0.0.0");
        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("/custom/plugins/path"));
        assertThat(args.isProxyProtocol()).isFalse();
        assertThat(args.getDefaultLanguage()).isEqualTo("zh-CN");
    }

    @Test
    @DisplayName("Should parse custom proxy protocol from arguments")
    void shouldParseCustomProxyProtocolFromArguments() {
        ServerArgs args = new ServerArgs(new String[]{"--proxy-protocol", "true"});

        assertThat(args.getPort()).isEqualTo(12346);
        assertThat(args.getHost()).isEqualTo("0.0.0.0");
        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("plugins"));
        assertThat(args.isProxyProtocol()).isTrue();
        assertThat(args.getDefaultLanguage()).isEqualTo("zh-CN");
    }

    @Test
    @DisplayName("Should parse custom language from arguments")
    void shouldParseCustomLanguageFromArguments() {
        ServerArgs args = new ServerArgs(new String[]{"--language", "en-US"});

        assertThat(args.getPort()).isEqualTo(12346);
        assertThat(args.getHost()).isEqualTo("0.0.0.0");
        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("plugins"));
        assertThat(args.isProxyProtocol()).isFalse();
        assertThat(args.getDefaultLanguage()).isEqualTo("en-US");
    }

    @Test
    @DisplayName("Should parse all custom arguments together")
    void shouldParseAllCustomArgumentsTogether() {
        ServerArgs args = new ServerArgs(new String[]{
                "--port", "9090",
                "--host", "192.168.1.1",
                "--plugins", "/var/plugins",
                "--proxy-protocol", "true",
                "--language", "en-GB"
        });

        assertThat(args.getPort()).isEqualTo(9090);
        assertThat(args.getHost()).isEqualTo("192.168.1.1");
        assertThat(args.getPluginsDir()).isEqualTo(Paths.get("/var/plugins"));
        assertThat(args.isProxyProtocol()).isTrue();
        assertThat(args.getDefaultLanguage()).isEqualTo("en-GB");
    }
}
