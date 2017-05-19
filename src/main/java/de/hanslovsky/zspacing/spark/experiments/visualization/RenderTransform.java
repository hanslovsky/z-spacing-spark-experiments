package de.hanslovsky.zspacing.spark.experiments.visualization;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

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
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.DisplayMode;
import ij.ImagePlus;
import io.scif.img.ImgIOException;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

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

	public static void main( final String[] args ) throws JsonIOException, JsonSyntaxException, IOException, ImgIOException
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
			final ScaleOptions config = ScaleOptions.createFromFile( p.configPath + "/config.json" );
			final N5 n5 = new N5( p.n5path );
			final DatasetAttributes attr = n5.getDatasetAttributes( p.dataset );

			final DiskCachedCellImgOptions options = DiskCachedCellImgOptions
					.options()
					.cellDimensions( attr.getBlockSize() )
					.dirtyAccesses( false )
					.maxCacheSize( 100 );

			final DiskCachedCellImgFactory< UnsignedByteType > factory = new DiskCachedCellImgFactory<>( options );

			final DiskCachedCellImgFactory< FloatType > scaledTransformFactory = new DiskCachedCellImgFactory<>( options );

			final long[] dim = attr.getDimensions();
			final int[] cellSize = attr.getBlockSize();

			final CellGrid grid = new CellGrid( dim, cellSize );

			final int nIterations = config.radii.length;

			final String[] forwardTransformFormats = IntStream.range( 0, config.radii.length ).mapToObj( i -> String.format( "%s/out/%02d/forward/%s", p.configPath, i, "%04d.tif" ) ).toArray( String[]::new );
			final String[] backwardTransformFormats = IntStream.range( 0, config.radii.length ).mapToObj( i -> String.format( "%s/out/%02d/backward/%s", p.configPath, i, "%04d.tif" ) ).toArray( String[]::new );

			final Img< FloatType >[] forwardTransforms = new Img[ config.radii.length ];
			final Img< FloatType >[] backwardTransforms = new Img[ config.radii.length ];

			final Img< FloatType >[] scaledForwardTransforms = new Img[ config.radii.length ];
			final Img< FloatType >[] scaledBackwardTransforms = new Img[ config.radii.length ];

			final long scaleToSource = 1 << config.scale;

			{
//				final ImgOpener opener = new ImgOpener();

				for ( int i = 0; i < nIterations; ++i ) {

					final double[] scales = Arrays.stream( config.steps[ i ] ).mapToDouble( val -> val ).toArray();
					final double[] translations = Arrays.stream( config.radii[ i ] ).mapToDouble( val -> val ).toArray();


					final String forwardFormat = forwardTransformFormats[ i ];
					final String backwardFormat = backwardTransformFormats[ i ];
					final Img< FloatType > firstImg = ImageJFunctions.wrapFloat( new ImagePlus( String.format( forwardFormat, 0 ) ) );
//					final SCIFIOImgPlus< ? > firstImg = opener.openImg( String.format( forwardFormat, 0 ) );
					final long width = firstImg.dimension( 0 );
					final long height = firstImg.dimension( 1 );

					final int[] transformCellSize = new int[] { ( int ) width, ( int ) height, 1 };
					final long[] transformDim = new long[] { width, height, dim[ 2 ] };

					final DiskCachedCellImgOptions scaledOptions = DiskCachedCellImgOptions
							.options()
							.cellDimensions( transformCellSize )
							.dirtyAccesses( false )
							.maxCacheSize( 100 );
					final DiskCachedCellImgFactory< FloatType > transformFactory = new DiskCachedCellImgFactory<>( scaledOptions );

					final CellLoader< FloatType > forwardLoader = img -> {
						burnInSubtract( ImageJFunctions.wrapFloat( new ImagePlus( String.format( forwardFormat, img.min( 2 ) ) ) ), img, new FloatType( img.min( 2 ) ) );
					};

					final CellLoader< FloatType > backwardLoader = img -> {
						burnInSubtract( ImageJFunctions.wrapFloat( new ImagePlus( String.format( backwardFormat, img.min( 2 ) ) ) ), img, new FloatType( img.min( 2 ) ) );
					};

					final DiskCachedCellImg< FloatType, ? > forward = transformFactory.create( transformDim, new FloatType(), forwardLoader );
					final DiskCachedCellImg< FloatType, ? > backward = transformFactory.create( transformDim, new FloatType(), backwardLoader );

					final ScaleAndTranslation scaleAndTranslate = new ScaleAndTranslation( scales, translations );
					final Scale2D scale = new Scale2D( scaleToSource, scaleToSource );

					final CellLoader< FloatType > scaledForwardLoader = img -> {
						for ( long z = img.min( 2 ); z <= img.max( 2 ); ++z )
						{
							final IntervalView< FloatType > sourceHS = Views.hyperSlice( forward, 2, z );
							final IntervalView< FloatType > target = Views.hyperSlice( img, 2, z );
							final RealRandomAccessible< FloatType > extendedAndInterpolated = Views.interpolate( Views.extendBorder( sourceHS ), new NLinearInterpolatorFactory<>() );
							final RealTransformRandomAccessible< FloatType, InverseRealTransform > tfed1 = RealViews.transform( extendedAndInterpolated, scaleAndTranslate );
							final RealTransformRandomAccessible< FloatType, InverseRealTransform > tfed2 = RealViews.transform( tfed1, scale );
							burnIn( Views.interval( Views.raster( tfed2 ), target ), target );
						}
					};

					forwardTransforms[ i ] = forward;
					backwardTransforms[ i ] = backward;

					final DiskCachedCellImg< FloatType, ? > scaledForward = scaledTransformFactory.create( dim, new FloatType(), scaledForwardLoader );

					scaledForwardTransforms[ i ] = scaledForward;
				}
//				opener.context().dispose();
			}

			System.out.println( attr.getNumDimensions() + " " + attr.getDataType() + " " + attr.getCompressionType() + " " + Arrays.toString( attr.getDimensions() ) + " " + Arrays.toString( attr.getBlockSize() ) );

			final int numProc = Runtime.getRuntime().availableProcessors();
			final SharedQueue queue = new SharedQueue( numProc - 1 );

			final N5Loader< UnsignedByteType > n5loader = new N5Loader<>( n5, p.dataset, attr.getBlockSize() );

			final DiskCachedCellImg< UnsignedByteType, ? > img = factory.create( dim, new UnsignedByteType(), n5loader );

			final BdvStackSource< Volatile< UnsignedByteType > > bdv = BdvFunctions.show( VolatileViews.wrapAsVolatile( img, queue ), "source" );
			bdv.getBdvHandle().getSetupAssignments().getMinMaxGroups().get( 0 ).setRange( 0, 255 );
			bdv.setDisplayRange( 0, 255 );
			bdv.getBdvHandle().getViewerPanel().setDisplayMode( DisplayMode.SINGLE );

			for ( int i = 0; i < nIterations; ++i )
			{
				BdvFunctions.show( VolatileViews.wrapAsVolatile( scaledForwardTransforms[ i ], queue ), "forward", BdvOptions.options().addTo( bdv ) );
				bdv.getBdvHandle().getSetupAssignments().getMinMaxGroups().get( i + 1 ).setRange( 10, 30 );
			}


		}

	}

	public static < T extends RealType< T > > void burnInSubtract( final RandomAccessibleInterval< T > source, final RandomAccessibleInterval< T > target, final T val )
	{
		for ( Cursor< T > s = Views.flatIterable( source ).cursor(), t = Views.flatIterable( target ).cursor(); s.hasNext(); )
		{
			final T var = t.next();
			var.set( s.next() );
			var.sub( val );
		}
	}

	public static < T extends RealType< T > > void burnIn( final RandomAccessibleInterval< T > source, final RandomAccessibleInterval< T > target )
	{
		for ( Cursor< T > s = Views.flatIterable( source ).cursor(), t = Views.flatIterable( target ).cursor(); s.hasNext(); )
			t.next().setReal( s.next().getRealDouble() + 20 );
	}

}
