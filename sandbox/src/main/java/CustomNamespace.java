import helix.system.cli.CliNamespace;

public class CustomNamespace extends CliNamespace
{
    public CustomNamespace()
    {
        registerCommand(new CustomCommand());
    }

    public String description() {
        return "Just an example CLI namespace added by Genome sandbox";
    }

    public String name() {
        return "custom-namespace";
    }
}
