package de.hanslovsky.zspacing.spark.experiments.visualization;

import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5;
import org.janelia.saalfeldlab.n5.cache.N5Loader;
import org.janelia.thickness.ScaleOptions;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import net.imglib2.Volatile;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class RenderTransform
{

	public static class Parameters
	{

		@Argument( metaVar = "CONFIG_PATH", required = true )
		private String configPath;

		@Option( name = "-n", aliases = { "--n5-path" }, metaVar = "N5_PATH", required = true )
		private String n5path;

		@Option( name = "-d", aliases = { "--n5-dataset" }, metaVar = "N5_DATASET", required = true )
		private String dataset;

		private boolean parsedSuccessfully = false;

	}

	public static void main( final String[] args ) throws JsonIOException, JsonSyntaxException, IOException
	{
		final Parameters p = new Parameters();
		final CmdLineParser parser = new CmdLineParser( p );
		try
		{
			parser.parseArgument( args );
			p.parsedSuccessfully = true;
		}
		catch ( final CmdLineException e )
		{
			System.err.println( e.getMessage() );
			parser.printUsage( System.err );
			p.parsedSuccessfully = false;
		}

		if ( p.parsedSuccessfully ) {
			final ScaleOptions config = ScaleOptions.createFromFile( p.configPath );
			final N5 n5 = new N5( p.n5path );
			final DatasetAttributes attr = n5.getDatasetAttributes( p.dataset );

			final long[] dim = attr.getDimensions();
			final int[] cellSize = attr.getBlockSize();

			final CellGrid grid = new CellGrid( dim, cellSize );

			System.out.println( attr.getNumDimensions() + " " + attr.getDataType() + " " + attr.getCompressionType() + " " + Arrays.toString( attr.getDimensions() ) + " " + Arrays.toString( attr.getBlockSize() ) );

			final int numProc = Runtime.getRuntime().availableProcessors();
			final SharedQueue queue = new SharedQueue( numProc - 1 );

			final N5Loader< UnsignedByteType > n5loader = new N5Loader<>( n5, p.dataset, attr.getBlockSize() );

			final DiskCachedCellImgOptions options = DiskCachedCellImgOptions
					.options()
					.cellDimensions( attr.getBlockSize() )
					.dirtyAccesses( false )
					.maxCacheSize( 100 );

			final DiskCachedCellImgFactory< UnsignedByteType > factory = new DiskCachedCellImgFactory<>( options );

			final DiskCachedCellImg< UnsignedByteType, ? > img = factory.create( dim, new UnsignedByteType(), n5loader );

			final BdvStackSource< Volatile< UnsignedByteType > > bdv = BdvFunctions.show( VolatileViews.wrapAsVolatile( img, queue ), "source" );

			bdv.setDisplayRange( 0, 255 );


		}
	}

}
