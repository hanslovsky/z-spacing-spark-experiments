package de.hanslovsky.zspacing.spark.experiments.mask;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

public class IgnoreFilterMaskGenerator< T, R extends RealType< R > > implements MaskGenerator< T, R >
{

	public static interface Filter< T >
	{
		boolean accept( T t );
	}

	private final Filter< T > filter;

	public IgnoreFilterMaskGenerator( final Filter< T > filter )
	{
		super();
		this.filter = filter;
	}

	@Override
	public void generateMask( final RandomAccessible< T > image, final RandomAccessibleInterval< R > mask )
	{
		for ( final Pair< T, R > p : Views.interval( Views.pair( image, mask ), mask ) )
			p.getB().setReal( filter.accept( p.getA() ) ? 1.0 : 0.0 );
	}

}
