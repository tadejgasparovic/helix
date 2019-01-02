package helix.system.cli.namespaces;

import helix.system.cli.CliNamespace;
import helix.system.cli.commands.sequencer.ListGenomes;
import helix.system.cli.commands.sequencer.LoadGenome;

public class GenomeSequencer extends CliNamespace
{
    public GenomeSequencer()
    {
        registerCommand(new ListGenomes());
        registerCommand(new LoadGenome());
    }

    @Override
    public String description()
    {
        return "All the tools you need to manage your Genomes!";
    }

    @Override
    public String name()
    {
        return "sequencer";
    }
}
