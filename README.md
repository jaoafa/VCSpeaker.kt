# VCSpeaker.kt

🔊 Text channel, now with voice - VCSpeaker

## Getting Started

### Run VCSpeaker locally

1. Download the latest release from [Releases](https://github.com/jaoafa/VCSpeaker.kt/releases).
2. Put the jar file in the new folder (e.g. `vcspeaker`)
3. Create `config.yml` in the same folder as the jar file.
4. Write the following in `config.yml`:
    ```yaml
    token:
      discord: <Discord Bot Token>
      voicetext: <VoiceText API Token>
    env:
      dev: <The id of the guild for development. null to disable development mode.>
      cache_policy: 3
      command_prefix: '$'
    ```
5. Run the jar file with `java -jar vcspeaker-kt.jar`
6. Invite the bot to your server.
7. Run `/vcspeaker settings channel:<TextChannel>` to set the text channel to be read aloud.
8. Now, when you send a message in the text channel, the bot will read it aloud in the voice channel.

## About Versioning

For official releases, we follow [Semantic Versioning 2.0.0](https://semver.org/).  
In development environments, the following rules apply:

- If you run the code directly (e.g., using "Run 'Main.kt'" in IntelliJ IDEA), the version will be `local-run-<TIMESTAMP>`.  
  Example: `local-run-1742826473762`
- If you build a JAR file locally, the version will be `local-build-<TIMESTAMP>`.  
  Example: `local-build-1742826473762`
- If you build a docker image locally, the version will be `local-docker`.  
  Example: `local-docker`

Every version, other than local run, will have its version in `MANIFEST.MF` as the entry `VCSpeaker-Version`.  
Version information can be obtained from `VCSpeaker.version` in the code.  
You can specify a custom version by building with the `-Pversion` option:

```shell
gradle build -Pversion=custom-version
```