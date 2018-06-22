# Eclipse Java Development Tooling Language Server Client (JDT-LS-4e)

A language server client for Eclipse using JDT-LS.

## Running The Application

1. eclipse.jdt.ls must have been built successfully locally

        $ mvn -DskipTests verify

2. Import `org.eclipse.jdt.ls.client` and `org.eclipse.jdt.ls.client.target` into your workspace
3. Under Window -> Preferences, Plugin-in Development -> Target Platform, enable `jdt-ls-client` as the target platform.
4. Launch a child Eclipse run configuration with that includes `org.eclipse.jdt.ls.client` and ensure the property/environment variable `jdt.ls.home` must is set to the location of the JDT LS project folder
