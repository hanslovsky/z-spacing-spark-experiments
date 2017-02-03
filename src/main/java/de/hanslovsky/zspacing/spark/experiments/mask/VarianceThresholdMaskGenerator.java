package de.hanslovsky.zspacing.spark.experiments.mask;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.integral.IntegralImgDouble;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealDoubleConverter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class VarianceThresholdMaskGenerator< T extends RealType< T >, R extends RealType< R > > implements MaskGenerator< T, R >
{

	private final IgnoreFilterMaskGenerator< T, R > generator;

	private final long[] windowRadius;

	private final long[] min;

	private final long[] max;

	public VarianceThresholdMaskGenerator( final double threshold, final long... windowRadius )
	{
		super();
		this.generator = new IgnoreFilterMaskGenerator<>( t -> t.getRealDouble() > threshold );
		this.windowRadius = windowRadius;
		this.min = new long[ windowRadius.length ];
		this.max = new long[ windowRadius.length ];
	}

	@Override
	public void generateMask( final RandomAccessible< T > image, final RandomAccessibleInterval< R > mask )
	{

		assert image.numDimensions() == this.windowRadius.length;

		final Img< DoubleType > sumX = getIntegralImg( Views.interval( image, mask ), new RealDoubleConverter<>() );
		final Img< DoubleType > sumXX = getIntegralImg( Views.interval( image, mask ), ( s, t ) -> {
			final double v = s.getRealDouble();
			t.set( v * v );
		} );

		final long[] dims = Intervals.dimensionsAsLongArray( mask );
		final long[] position = new long[ dims.length ];
		final long[] imgMax = Intervals.maxAsLongArray( mask );

		final RandomAccess< R > maskAccess = mask.randomAccess();

		for ( int d = 0; d < dims.length; )
		{
			int size = 1;
			for ( int k = 0; k < dims.length; ++k ) {
				min[ d ] = Math.max( position[d] - windowRadius[d], 0 );
				max[ d ] = Math.min( position[ d ] + windowRadius[ d ] , imgMax[ d ] );
				size *= max[ d ] - min[ d ];
			}
			maskAccess.setPosition( position );

			final double sX = getIntegral( sumX, min, max );
			final double var = getIntegral( sumXX, min, max ) / size - sX * sX / size / size;
			maskAccess.get().setReal( var > 500 && var < 2000 ? 1.0 : 0.0 );


			for ( d = 0; d < dims.length; ++d )
			{
				++position[ d ];
				if ( position[ d ] < dims[ d ] )
					break;
				else
					position[ d ] = 0;
			}
		}



		// generate integral image variance img
		//		this.generator.generateMask( image, mask );
	}

	private static < T extends NumericType< T > > Img< DoubleType > getIntegralImg( final RandomAccessibleInterval< T > image, final Converter< T, DoubleType > conv )
	{
		final IntegralImgDouble ii = new IntegralImgDouble<>( image, new DoubleType(), conv );
		ii.process();
		return ii.getResult();
	}

	public static double getIntegral( final RandomAccessibleInterval< ? extends RealType< ? > > integralImg, final long[] min, final long[] max )
	{

		assert max.length == 2: "Currently only two dimensions allowed!";

		final RandomAccess< ? extends RealType< ? > > ra = integralImg.randomAccess();

		double sum = 0.0;

		ra.setPosition( min );
		sum += ra.get().getRealDouble();

		ra.setPosition( max );
		sum += ra.get().getRealDouble();

		ra.setPosition( min[ 0 ], 0 );
		ra.setPosition( max[ 1 ], 1 );
		sum -= ra.get().getRealDouble();

		ra.setPosition( min[ 1 ], 1 );
		ra.setPosition( max[ 0 ], 0 );
		sum -= ra.get().getRealDouble();

		return sum;
	}

}
