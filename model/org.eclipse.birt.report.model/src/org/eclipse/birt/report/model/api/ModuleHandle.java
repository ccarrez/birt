/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.model.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.command.CustomMsgException;
import org.eclipse.birt.report.model.api.core.DisposeEvent;
import org.eclipse.birt.report.model.api.core.AttributeEvent;
import org.eclipse.birt.report.model.api.core.IDisposeListener;
import org.eclipse.birt.report.model.api.core.IAttributeListener;
import org.eclipse.birt.report.model.api.core.IModuleModel;
import org.eclipse.birt.report.model.api.elements.structures.ConfigVariable;
import org.eclipse.birt.report.model.api.elements.structures.CustomColor;
import org.eclipse.birt.report.model.api.elements.structures.EmbeddedImage;
import org.eclipse.birt.report.model.api.metadata.IElementDefn;
import org.eclipse.birt.report.model.api.metadata.MetaDataConstants;
import org.eclipse.birt.report.model.api.metadata.PropertyValueException;
import org.eclipse.birt.report.model.api.util.StringUtil;
import org.eclipse.birt.report.model.api.validators.IValidationListener;
import org.eclipse.birt.report.model.api.validators.ValidationEvent;
import org.eclipse.birt.report.model.command.CustomMsgCommand;
import org.eclipse.birt.report.model.command.PropertyCommand;
import org.eclipse.birt.report.model.command.ShiftLibraryCommand;
import org.eclipse.birt.report.model.core.CachedMemberRef;
import org.eclipse.birt.report.model.core.ContainerSlot;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.core.Module;
import org.eclipse.birt.report.model.core.Structure;
import org.eclipse.birt.report.model.core.StyleElement;
import org.eclipse.birt.report.model.elements.Library;
import org.eclipse.birt.report.model.elements.ReportDesign;
import org.eclipse.birt.report.model.elements.Translation;
import org.eclipse.birt.report.model.metadata.ElementPropertyDefn;
import org.eclipse.birt.report.model.writer.DesignWriter;

/**
 * Abstract module handle which provides the common functionalities of report
 * design and library.
 * 
 * <table border="1" cellpadding="2" cellspacing="2" style="border-collapse:
 * collapse" bordercolor="#111111">
 * <th width="20%">Content Item</th>
 * <th width="40%">Description</th>
 * 
 * <tr>
 * <td>Code Modules</td>
 * <td>Global scripts that apply to the report as a whole.</td>
 * </tr>
 * 
 * <tr>
 * <td>Parameters</td>
 * <td>A list of Parameter elements that describe the data that the user can
 * enter when running the report.</td>
 * </tr>
 * 
 * <tr>
 * <td>Data Sources</td>
 * <td>The connections used by the report.</td>
 * </tr>
 * 
 * <tr>
 * <td>Data Sets</td>
 * <td>Data sets defined in the design.</td>
 * </tr>
 * 
 * <tr>
 * <td>Color Palette</td>
 * <td>A set of custom color names as part of the design.</td>
 * </tr>
 * 
 * <tr>
 * <td>Styles</td>
 * <td>User-defined styles used to format elements in the report. Each style
 * must have a unique name within the set of styles for this report.</td>
 * </tr>
 * 
 * <tr>
 * <td>Page Setup</td>
 * <td>The layout of the master pages within the report.</td>
 * </tr>
 * 
 * <tr>
 * <td>Components</td>
 * <td>Reusable report items defined in this design. Report items can extend
 * these items. Defines a "private library" for this design.</td>
 * </tr>
 * 
 * <tr>
 * <td>Translations</td>
 * <td>The list of externalized messages specifically for this report.</td>
 * </tr>
 * 
 * <tr>
 * <td>Images</td>
 * <td>A list of images embedded in this report.</td>
 * </tr>
 * 
 * </table>
 */

public abstract class ModuleHandle extends DesignElementHandle
		implements
			IModuleModel
{

	/**
	 * Constructs one module handle with the given module element.
	 * 
	 * @param module
	 *            module
	 */

	public ModuleHandle( Module module )
	{
		super( module );
	}

	/**
	 * Adds a new config variable.
	 * 
	 * @param configVar
	 *            the config variable
	 * @throws SemanticException
	 *             if the name is empty or the same name exists.
	 *  
	 */

	public void addConfigVariable( ConfigVariable configVar )
			throws SemanticException
	{
		ElementPropertyDefn propDefn = module
				.getPropertyDefn( CONFIG_VARS_PROP );

		if ( configVar != null && StringUtil.isBlank( configVar.getName( ) ) )
		{
			throw new PropertyValueException( getElement( ), propDefn,
					configVar,
					PropertyValueException.DESIGN_EXCEPTION_INVALID_VALUE );
		}

		if ( configVar != null
				&& findConfigVariable( configVar.getName( ) ) != null )
		{
			throw new PropertyValueException( getElement( ), propDefn,
					configVar.getName( ),
					PropertyValueException.DESIGN_EXCEPTION_VALUE_EXISTS );
		}

		PropertyCommand cmd = new PropertyCommand( module, getElement( ) );
		cmd.addItem( new CachedMemberRef( propDefn ), configVar );
	}

	/**
	 * Adds a new embedded image.
	 * 
	 * @param image
	 *            the image to add
	 * @throws SemanticException
	 *             if the name is empty, type is invalid, or the same name
	 *             exists.
	 */

	public void addImage( EmbeddedImage image ) throws SemanticException
	{
		PropertyCommand cmd = new PropertyCommand( module, getElement( ) );
		ElementPropertyDefn propDefn = module.getPropertyDefn( IMAGES_PROP );
		cmd.addItem( new CachedMemberRef( propDefn ), image );
	}

	/**
	 * Adds all the parameters under the given parameter group to a list.
	 * 
	 * @param list
	 *            the list to which the parameters are added.
	 * @param handle
	 *            the handle to the parameter group.
	 */

	private void addParameters( ArrayList list, ParameterGroupHandle handle )
	{
		SlotHandle h = handle.getParameters( );
		Iterator it = h.iterator( );
		while ( it.hasNext( ) )
		{
			list.add( it.next( ) );
		}
	}

	/**
	 * Adds a new translation to the design.
	 * 
	 * @param resourceKey
	 *            resource key for the message
	 * @param locale
	 *            the string value of a locale for the translation. Locale
	 *            should be in java-defined format( en, en-US, zh_CN, etc.)
	 * @param text
	 *            translated text for the locale
	 * 
	 * @throws CustomMsgException
	 *             if the resource key is duplicate or missing, or locale is not
	 *             a valid format.
	 * 
	 * @see #getTranslation(String, String)
	 */

	public void addTranslation( String resourceKey, String locale, String text )
			throws CustomMsgException
	{
		CustomMsgCommand command = new CustomMsgCommand( getModule( ) );
		command.addTranslation( resourceKey, locale, text );
	}

	/**
	 * Adds the validation listener, which implements
	 * <code>IValidationListener</code>. A listener receives notifications
	 * each time an element is validated.
	 * 
	 * @param listener
	 *            the validation listener.
	 */

	public void addValidationListener( IValidationListener listener )
	{
		getModule( ).addValidationListener( listener );
	}

	/**
	 * Checks this whole report. Only one <code>ValidationEvent</code> will be
	 * sent, which contains all error information of this check.
	 */

	public void checkReport( )
	{
		// validate the whole design

		module.semanticCheck( module );

		ValidationEvent event = new ValidationEvent( module, null,
				getErrorList( ) );

		module.broadcastValidationEvent( module, event );
	}

	/**
	 * Closes the design. The report design handle is no longer valid after
	 * closing the design. This method will send notifications instance of
	 * <code>DisposeEvent</code> to all the dispose listeners registered in
	 * the module.
	 */

	public void close( )
	{
		module.close( );
		DisposeEvent event = new DisposeEvent( module );
		module.broadcastDisposeEvent( event );
	}

	/**
	 * Returns the structures which are defined in report design and all
	 * included valid libraries. This method will filter the structure with
	 * duplicate name with the follow rule.
	 * 
	 * <ul>
	 * <li>The structure defined in design file overrides the one with the same
	 * name in library file.
	 * <li>The structure defined in preceding library overrides the one with
	 * the same name in following library file.
	 * <ul>
	 * 
	 * @param propName
	 *            name of the list property
	 * @param nameMember
	 *            name of the name member
	 * @return the filtered structure list with the above rule.
	 */

	List getFilteredStructureList( String propName, String nameMember )
	{
		List list = new ArrayList( );

		PropertyHandle propHandle = getPropertyHandle( propName );
		assert propHandle != null;

		Set names = new HashSet( );
		Iterator iter = propHandle.iterator( );
		while ( iter.hasNext( ) )
		{
			StructureHandle s = (StructureHandle) iter.next( );
			String nameValue = (String) s.getProperty( nameMember );
			if ( !names.contains( nameValue ) )
			{
				list.add( s );
				names.add( nameValue );
			}
		}

		List theLibraries = getLibraries( );
		int size = theLibraries.size( );
		for ( int i = 0; i < size; i++ )
		{
			LibraryHandle library = (LibraryHandle) theLibraries.get( i );
			if ( library.isValid( ) )
			{
				iter = library.getFilteredStructureList( propName, nameMember )
						.iterator( );
				while ( iter.hasNext( ) )
				{
					StructureHandle s = (StructureHandle) iter.next( );
					String nameValue = (String) s.getProperty( nameMember );
					if ( !names.contains( nameValue ) )
					{
						list.add( s );
						names.add( nameValue );
					}
				}
			}
		}

		return list;
	}

	/**
	 * Returns the structures which are defined in report design and all
	 * included valid libraries. This method will collect all structures from
	 * design file and each valid library.
	 * 
	 * @param propName
	 *            name of the list property
	 * @return the structure list, each of which is the instance of
	 *         <code>StructureHandle</code>
	 */

	List getStructureList( String propName )
	{
		List list = new ArrayList( );

		PropertyHandle propHandle = getPropertyHandle( propName );
		assert propHandle != null;

		Iterator iter = propHandle.iterator( );
		while ( iter.hasNext( ) )
		{
			StructureHandle s = (StructureHandle) iter.next( );
			list.add( s );
		}

		List theLibraries = getLibraries( );
		int size = theLibraries.size( );
		for ( int i = 0; i < size; i++ )
		{
			LibraryHandle library = (LibraryHandle) theLibraries.get( i );
			if ( library.isValid( ) )
			{
				list.addAll( library.getStructureList( propName ) );
			}
		}

		return list;
	}

	/**
	 * Returns the iterator over all config variables. Each one is the instance
	 * of <code>ConfigVariableHandle</code>.
	 * <p>
	 * Note: The configure variable in library file will be hidden if the one
	 * with the same name appears in design file.
	 * 
	 * @return the iterator over all config variables.
	 * @see ConfigVariableHandle
	 */

	public Iterator configVariablesIterator( )
	{
		return getFilteredStructureList( CONFIG_VARS_PROP,
				ConfigVariable.NAME_MEMBER ).iterator( );
	}

	/**
	 * Returns the iterator over all structures of color palette. Each one is
	 * the instance of <code>CustomColorHandle</code>
	 * 
	 * @return the iterator over all structures of color palette.
	 * @see CustomColorHandle
	 */

	public Iterator customColorsIterator( )
	{
		return getStructureList( COLOR_PALETTE_PROP ).iterator( );
	}

	/**
	 * Drops a config variable.
	 * 
	 * @param name
	 *            config variable name
	 * @throws SemanticException
	 *             if no config variable is found.
	 * @deprecated
	 */

	public void dropConfigVariable( String name ) throws SemanticException
	{
		PropertyHandle propHandle = this.getPropertyHandle( CONFIG_VARS_PROP );

		int posn = findConfigVariablePos( name );
		if ( posn < 0 )
			throw new PropertyValueException( getElement( ), propHandle
					.getPropertyDefn( ), name,
					PropertyValueException.DESIGN_EXCEPTION_ITEM_NOT_FOUND );

		propHandle.removeItem( posn );

	}

	/**
	 * Drops an embedded image handle list from the design. Each one in the list
	 * is the instance of <code>EmbeddedImageHandle</code>.
	 * 
	 * @param images
	 *            the image handle list to remove
	 * @throws SemanticException
	 *             if any image in the list is not found.
	 */

	public void dropImage( List images ) throws SemanticException
	{
		if ( images == null )
			return;
		PropertyHandle propHandle = this.getPropertyHandle( IMAGES_PROP );
		propHandle.removeItems( images );
	}

	/**
	 * Drops an embedded image from the design.
	 * 
	 * @param name
	 *            the image name
	 * @throws SemanticException
	 *             if the image is not found.
	 * @deprecated
	 */

	public void dropImage( String name ) throws SemanticException
	{
		PropertyHandle propHandle = this.getPropertyHandle( IMAGES_PROP );

		int pos = findImagePos( name );
		if ( pos < 0 )
			throw new PropertyValueException( getElement( ), propHandle
					.getPropertyDefn( ), name,
					PropertyValueException.DESIGN_EXCEPTION_ITEM_NOT_FOUND );

		propHandle.removeItem( pos );
	}

	/**
	 * Drops a translation from the design.
	 * 
	 * @param resourceKey
	 *            resource key of the message in which this translation saves.
	 * @param locale
	 *            the string value of the locale for a translation. Locale
	 *            should be in java-defined format( en, en-US, zh_CN, etc.)
	 * @throws CustomMsgException
	 *             if <code>resourceKey</code> is <code>null</code>.
	 * @see #getTranslation(String, String)
	 */

	public void dropTranslation( String resourceKey, String locale )
			throws CustomMsgException
	{
		CustomMsgCommand command = new CustomMsgCommand( getModule( ) );
		command.dropTranslation( resourceKey, locale );
	}

	/**
	 * Finds the position of the config variable with the given name.
	 * 
	 * @param name
	 *            the config variable name
	 * @return the index ( from 0 ) of config variable with the given name.
	 *         Return -1, if not found.
	 *  
	 */

	private int findConfigVariablePos( String name )
	{
		List configVars = (List) module.getLocalProperty( module,
				CONFIG_VARS_PROP );
		if ( configVars == null )
			return -1;

		int i = 0;
		for ( Iterator iter = configVars.iterator( ); iter.hasNext( ); i++ )
		{
			ConfigVariable var = (ConfigVariable) iter.next( );

			if ( var.getName( ).equals( name ) )
			{
				return i;
			}
		}

		return -1;
	}

	/**
	 * Finds a data set by name in this module and the included modules.
	 * 
	 * @param name
	 *            name of the data set
	 * @return a handle to the data set, or <code>null</code> if the data set
	 *         is not found
	 */

	public DataSetHandle findDataSet( String name )
	{
		DesignElement element = module.findDataSet( name );
		if ( element == null )
			return null;
		return (DataSetHandle) element.getHandle( module );
	}

	/**
	 * Finds a data source by name in this module and the included modules.
	 * 
	 * @param name
	 *            name of the data source
	 * @return a handle to the data source, or <code>null</code> if the data
	 *         source is not found
	 */

	public DataSourceHandle findDataSource( String name )
	{
		DesignElement element = module.findDataSource( name );
		if ( element == null )
			return null;
		return (DataSourceHandle) element.getHandle( module );
	}

	/**
	 * Finds a named element in the name space in this module and the included
	 * moduled.
	 * 
	 * @param name
	 *            the name of the element to find
	 * @return a handle to the element, or <code>null</code> if the element
	 *         was not found.
	 */

	public DesignElementHandle findElement( String name )
	{
		DesignElement element = module.findElement( name );
		if ( element == null )
			return null;
		return element.getHandle( module );
	}

	/**
	 * Finds the image with the given name.
	 * 
	 * @param name
	 *            the image name
	 * @return embedded image with the given name. Return <code>null</code>,
	 *         if not found.
	 */

	public EmbeddedImage findImage( String name )
	{
		return module.findImage( name );
	}

	/**
	 * Finds the config variable with the given name.
	 * 
	 * @param name
	 *            the variable name
	 * @return the config variable with the given name. Return <code>null</code>,
	 *         if not found.
	 */

	public ConfigVariable findConfigVariable( String name )
	{
		return module.findConfigVariabel( name );
	}

	/**
	 * Finds the custom color with the given name.
	 * 
	 * @param name
	 *            the color name
	 * @return the custom color with the given name. Return <code>null</code>
	 *         if it's not found.
	 */

	public CustomColor findColor( String name )
	{
		return module.findColor( name );
	}

	/**
	 * Finds the position of the image with the given name.
	 * 
	 * @param name
	 *            the image name to find
	 * @return position of image with the specified name. Return -1, if not
	 *         found.
	 */

	private int findImagePos( String name )
	{
		List images = (List) module.getLocalProperty( module, IMAGES_PROP );

		int i = 0;
		for ( Iterator iter = images.iterator( ); iter.hasNext( ); i++ )
		{
			EmbeddedImage image = (EmbeddedImage) iter.next( );

			if ( image.getName( ) != null
					&& image.getName( ).equalsIgnoreCase( name ) )
			{
				return i;
			}
		}

		return -1;
	}

	/**
	 * Finds a master page by name in this module and the included modules.
	 * 
	 * @param name
	 *            the name of the master page
	 * @return a handle to the master page, or <code>null</code> if the page
	 *         is not found
	 */

	public MasterPageHandle findMasterPage( String name )
	{
		DesignElement element = module.findPage( name );
		if ( element == null )
			return null;
		return (MasterPageHandle) element.getHandle( module );
	}

	/**
	 * Finds a parameter by name in this module and the included modules.
	 * 
	 * @param name
	 *            the name of the parameter
	 * @return a handle to the parameter, or <code>null</code> if the
	 *         parameter is not found
	 */

	public ParameterHandle findParameter( String name )
	{
		DesignElement element = module.findParameter( name );
		if ( element == null )
			return null;
		return (ParameterHandle) element.getHandle( module );
	}

	/**
	 * Finds a style by its name in this module.
	 * 
	 * @param name
	 *            name of the style
	 * @return a handle to the style, or <code>null</code> if the style is not
	 *         found
	 */

	public SharedStyleHandle findNativeStyle( String name )
	{
		StyleElement style = module.findNativeStyle( name );
		if ( style == null )
			return null;
		return (SharedStyleHandle) style.getHandle( module );
	}

	/**
	 * Finds a style by its name in this module and the included modules.
	 * 
	 * @param name
	 *            name of the style
	 * @return a handle to the style, or <code>null</code> if the style is not
	 *         found
	 */

	public SharedStyleHandle findStyle( String name )
	{
		StyleElement style = module.findStyle( name );
		if ( style == null )
			return null;
		return (SharedStyleHandle) style.getHandle( module );
	}

	/**
	 * Returns the name of the author of the design report.
	 * 
	 * @return the name of the author.
	 */

	public String getAuthor( )
	{
		return getStringProperty( AUTHOR_PROP );
	}

	/**
	 * Returns the command stack that manages undo/redo operations for the
	 * design.
	 * 
	 * @return a command stack
	 * 
	 * @see CommandStack
	 */

	public CommandStack getCommandStack( )
	{
		return module.getActivityStack( );
	}

	/**
	 * Returns a slot handle to work with the top-level components within the
	 * report.
	 * 
	 * @return A handle for working with the components.
	 */

	public SlotHandle getComponents( )
	{
		return getSlot( COMPONENT_SLOT );
	}

	/**
	 * Returns the name of the tool that created the design.
	 * 
	 * @return the name of the tool
	 */

	public String getCreatedBy( )
	{
		return getStringProperty( CREATED_BY_PROP );
	}

	/**
	 * Returns a slot handle to work with the data sets within the report. Note
	 * that the order of the data sets within the slot is unimportant.
	 * 
	 * @return A handle for working with the data sets.
	 */

	public SlotHandle getDataSets( )
	{
		return getSlot( DATA_SET_SLOT );
	}

	/**
	 * Returns a slot handle to work with the data sources within the report.
	 * Note that the order of the data sources within the slot is unimportant.
	 * 
	 * @return A handle for working with the data sources.
	 */

	public SlotHandle getDataSources( )
	{
		return getSlot( DATA_SOURCE_SLOT );
	}

	/**
	 * Returns the default units for the design. These are the units that are
	 * used for dimensions that don't explicitly specify units.
	 * 
	 * @return the default units for the design.
	 * @see org.eclipse.birt.report.model.api.metadata.DimensionValue
	 */

	public String getDefaultUnits( )
	{
		return ( (ReportDesign) module ).getUnits( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.api.DesignElementHandle#getElement()
	 */
	public DesignElement getElement( )
	{
		return module;
	}

	/**
	 * Finds the handle to an element by a given element ID. Returns
	 * <code>null</code> if the ID is not valid, or if this session does not
	 * use IDs.
	 * 
	 * @param id
	 *            ID of the element to find
	 * @return A handle to the element, or <code>null</code> if the element
	 *         was not found or this session does not use IDs.
	 */

	public DesignElementHandle getElementByID( int id )
	{
		DesignElement element = module.getElementByID( id );
		if ( element == null )
			return null;
		return element.getHandle( module );
	}

	/**
	 * Returns a list containing errors during parsing the design file.
	 * 
	 * @return a list containing parsing errors. Each element in the list is
	 *         <code>ErrorDetail</code>.
	 * 
	 * @see ErrorDetail
	 */

	public List getErrorList( )
	{
		return module.getErrorList( );
	}

	/**
	 * Returns the file name of the design. This is the name of the file from
	 * which the design was read, or the name to which the design was last
	 * written.
	 * 
	 * @return the file name
	 */

	public String getFileName( )
	{
		return module.getFileName( );
	}

	/**
	 * Returns the flatten Parameters/ParameterGroups of the design. This method
	 * put all Parameters and ParameterGroups into a list then return it. The
	 * return list is sorted by on the display name of the parameters.
	 * 
	 * @return the sorted, flatten parameters and parameter groups.
	 */

	public List getFlattenParameters( )
	{
		ArrayList list = new ArrayList( );
		SlotHandle slotHandle = getParameters( );
		Iterator it = slotHandle.iterator( );
		while ( it.hasNext( ) )
		{
			DesignElementHandle h = (DesignElementHandle) it.next( );
			list.add( h );
			if ( h instanceof ParameterGroupHandle )
			{
				addParameters( list, (ParameterGroupHandle) h );
			}
		}
		DesignElementHandle.doSort( list );
		return list;
	}

	/**
	 * Returns an external file that provides help information for the report.
	 * 
	 * @return the name of an external file
	 */

	public String getHelpGuide( )
	{
		return getStringProperty( HELP_GUIDE_PROP );
	}

	/**
	 * Returns the script called when the report starts executing.
	 * 
	 * @return the script called when the report starts executing
	 */

	public String getInitialize( )
	{
		return getStringProperty( INITIALIZE_METHOD );
	}

	/**
	 * Returns a slot handle to work with the master pages within the report.
	 * Note that the order of the master pages within the slot is unimportant.
	 * 
	 * @return A handle for working with the master pages.
	 */

	public SlotHandle getMasterPages( )
	{
		return getSlot( PAGE_SLOT );
	}

	/**
	 * Finds user-defined messages for the current thread's locale.
	 * 
	 * @param resourceKey
	 *            Resource key of the user-defined message.
	 * @return the corresponding locale-dependent messages. Return
	 *         <code>null</code> if resoueceKey is blank.
	 * @see #getMessage(String, Locale)
	 */

	public String getMessage( String resourceKey )
	{
		return getModule( ).getMessage( resourceKey );
	}

	/**
	 * Finds user-defined messages for the given locale.
	 * <p>
	 * First we look up in the report itself, then look into the referenced
	 * message file. Each search uses a reduced form of Java locale-driven
	 * search algorithm: Language&Country, language, default.
	 * 
	 * @param resourceKey
	 *            Resource key of the user defined message.
	 * @param locale
	 *            locale of message, if the input <code>locale</code> is
	 *            <code>null</code>, the locale for the current thread will
	 *            be used instead.
	 * @return the corresponding locale-dependent messages. Return
	 *         <code>null</code> if resoueceKey is blank.
	 */

	public String getMessage( String resourceKey, Locale locale )
	{
		return getModule( ).getMessage( resourceKey, locale );
	}

	/**
	 * Return a list of user-defined message keys. The list contained resource
	 * keys defined in the report itself and the keys defined in the referenced
	 * message files for the current thread's locale. The list returned contains
	 * no duplicate keys.
	 * 
	 * @return a list of user-defined message keys.
	 */

	public List getMessageKeys( )
	{
		return getDesign( ).getMessageKeys( );
	}

	/**
	 * Returns a slot handle to work with the top-level parameters and parameter
	 * groups within the report. The order that the items appear within the slot
	 * determines the order in which they appear in the "requester" UI.
	 * 
	 * @return A handle for working with the parameters and parameter groups.
	 */

	public SlotHandle getParameters( )
	{
		return getSlot( PARAMETER_SLOT );
	}

	/**
	 * Returns a slot handle to work with the styles within the report. Note
	 * that the order of the styles within the slot is unimportant.
	 * 
	 * @return A handle for working with the styles.
	 */

	public SlotHandle getStyles( )
	{
		return getSlot( STYLE_SLOT );
	}

	/**
	 * Gets a handle to deal with a translation. A translation is identified by
	 * its resourceKey and locale.
	 * 
	 * @param resourceKey
	 *            the resource key
	 * @param locale
	 *            the locale information
	 * 
	 * @return corresponding <code>TranslationHandle</code>. Or return
	 *         <code>null</code> if the translation is not found in the
	 *         design.
	 * 
	 * @see TranslationHandle
	 */

	public TranslationHandle getTranslation( String resourceKey, String locale )
	{
		Translation translation = module.findTranslation( resourceKey, locale );

		if ( translation != null )
			return translation.handle( getModule( ) );

		return null;
	}

	/**
	 * Returns a string array containing all the resource keys of user-defined
	 * translations for the report.
	 * 
	 * @return a string array containing message resource keys, return
	 *         <code>null</code> if there is no messages defined in the
	 *         design.
	 */

	public String[] getTranslationKeys( )
	{
		return getModule( ).getTranslationResourceKeys( );
	}

	/**
	 * Gets a list of translation defined on the report. The content of the list
	 * is the corresponding <code>TranslationHandle</code>.
	 * 
	 * @return a list containing TranslationHandles defined on the report or
	 *         <code>null</code> if the design has no any translations.
	 * 
	 * @see TranslationHandle
	 */

	public List getTranslations( )
	{
		List translations = getModule( ).getTranslations( );

		if ( translations == null )
			return null;

		List translationHandles = new ArrayList( );

		for ( int i = 0; i < translations.size( ); i++ )
		{
			translationHandles.add( ( (Translation) translations.get( i ) )
					.handle( getModule( ) ) );
		}

		return translationHandles;
	}

	/**
	 * Returns a list containing warnings during parsing the design file.
	 * 
	 * @return a list containing parsing warnings. Each element in the list is
	 *         <code>ErrorDetail</code>.
	 * 
	 * @see ErrorDetail
	 */

	public List getWarningList( )
	{
		return module.getWarningList( );
	}

	/**
	 * Returns the iterator over all embedded images. Each one is the instance
	 * of <code>EmbeddedImageHandle</code>
	 * 
	 * @return the iterator over all embedded images.
	 * 
	 * @see EmbeddedImageHandle
	 */

	public Iterator imagesIterator( )
	{
		return getStructureList( IMAGES_PROP ).iterator( );
	}

	/**
	 * Determines if the design has changed since it was last read from, or
	 * written to, the file. The dirty state reflects the action of the command
	 * stack. If the user saves the design and then changes it, the design is
	 * dirty. If the user then undoes the change, the design is no longer dirty.
	 * 
	 * @return <code>true</code> if the design has changed since the last load
	 *         or save; <code>false</code> if it has not changed.
	 */

	public boolean needsSave( )
	{
		return module.isDirty( );
	}

	/**
	 * Calls to inform a save is successful. Must be called after a successful
	 * completion of a save done using <code>serialize</code>.
	 */

	public void onSave( )
	{
		module.onSave( );
	}

	/**
	 * Removes a given validation listener. If the listener not registered, then
	 * the request is silently ignored.
	 * 
	 * @param listener
	 *            the listener to de-register
	 * @return <code>true</code> if <code>listener</code> is sucessfully
	 *         removed. Otherwise <code>false</code>.
	 */

	public boolean removeValidationListener( IValidationListener listener )
	{
		return getModule( ).removeValidationListener( listener );
	}

	/**
	 * Checks the element name in name space of this report.
	 * 
	 * <ul>
	 * <li>If the element name is required and duplicate name is found in name
	 * space, rename the element with a new unique name.
	 * <li>If the element name is not required, clear the name.
	 * </ul>
	 * 
	 * @param elementHandle
	 *            the element handle whose name is need to check.
	 */

	public void rename( DesignElementHandle elementHandle )
	{
		if ( elementHandle == null )
			return;

		IElementDefn defn = elementHandle.getElement( ).getDefn( );

		if ( defn.getNameOption( ) == MetaDataConstants.REQUIRED_NAME )
			module.makeUniqueName( elementHandle.getElement( ) );
		else
			elementHandle.getElement( ).setName( null );

		for ( int i = 0; i < defn.getSlotCount( ); i++ )
		{
			ContainerSlot slot = elementHandle.getElement( ).getSlot( i );

			if ( slot != null )
			{
				for ( int pos = 0; pos < slot.getCount( ); pos++ )
				{
					DesignElement innerElement = slot.getContent( pos );
					rename( innerElement.getHandle( module ) );
				}
			}
		}
	}

	/**
	 * Replaces the old config variable with the new one.
	 * 
	 * @param oldVar
	 *            the old config variable
	 * @param newVar
	 *            the new config variable
	 * @throws SemanticException
	 *             if the old config variable is not found or the name of new
	 *             one is empty.
	 *  
	 */

	public void replaceConfigVariable( ConfigVariable oldVar,
			ConfigVariable newVar ) throws SemanticException
	{
		replaceObjectInList( CONFIG_VARS_PROP, oldVar, newVar );
	}

	/**
	 * Replaces the old embedded image with the new one.
	 * 
	 * @param oldVar
	 *            the old embedded image
	 * @param newVar
	 *            the new embedded image
	 * @throws SemanticException
	 *             if the old image is not found or the name of new one is
	 *             empty.
	 */

	public void replaceImage( EmbeddedImage oldVar, EmbeddedImage newVar )
			throws SemanticException
	{
		replaceObjectInList( IMAGES_PROP, oldVar, newVar );
	}

	/**
	 * Replaces an old object in the structure list with the given new one.
	 * 
	 * @param propName
	 *            the name of the property that holds a structure list
	 * @param oldVar
	 *            an existed object in the list
	 * @param newVar
	 *            a new object
	 * @throws SemanticException
	 *             if the old object is not found or the name of new one is
	 *             empty.
	 */

	private void replaceObjectInList( String propName, Object oldVar,
			Object newVar ) throws SemanticException
	{
		ElementPropertyDefn propDefn = module.getPropertyDefn( propName );

		PropertyCommand cmd = new PropertyCommand( module, getElement( ) );
		cmd.replaceItem( new CachedMemberRef( propDefn ), (Structure) oldVar,
				(Structure) newVar );
	}

	/**
	 * Saves the module to an existing file name. Call this only when the file
	 * name has been set.
	 * 
	 * @throws IOException
	 *             if the file cannot be saved on the storage
	 * 
	 * @see #saveAs(String)
	 */

	public abstract void save( ) throws IOException;

	/**
	 * Saves the design to the file name provided. The file name is saved in the
	 * design, and subsequent calls to <code>save( )</code> will save to this
	 * new name.
	 * 
	 * @param newName
	 *            the new file name
	 * @throws IOException
	 *             if the file cannot be saved
	 * 
	 * @see #save()
	 */

	public void saveAs( String newName ) throws IOException
	{
		setFileName( newName );
		save( );
	}

	/**
	 * Writes the report design to the given output stream. The caller must call
	 * <code>onSave</code> if the save succeeds.
	 * 
	 * @param out
	 *            the output stream to which the design is written.
	 * @throws IOException
	 *             if the file cannot be written to the output stream
	 *             successfully.
	 */

	public void serialize( OutputStream out ) throws IOException
	{
		assert out != null;

		module.prepareToSave( );
		DesignWriter writer = new DesignWriter( (ReportDesign) module );
		writer.write( out );
		module.onSave( );
	}

	/**
	 * Sets the name of the author of the design report.
	 * 
	 * @param author
	 *            the name of the author.
	 */

	public void setAuthor( String author )
	{
		try
		{
			setStringProperty( AUTHOR_PROP, author );
		}
		catch ( SemanticException e )
		{
			assert false;
		}
	}

	/**
	 * Returns the name of the tool that created the design.
	 * 
	 * @param toolName
	 *            the name of the tool
	 */

	public void setCreatedBy( String toolName )
	{
		try
		{
			setStringProperty( CREATED_BY_PROP, toolName );
		}
		catch ( SemanticException e )
		{
			assert false;
		}
	}

	/**
	 * Sets the design file name. This method will send notifications instance
	 * of <code>AttributeEvent</code> to all the attribute listeners registered
	 * in the module.
	 * 
	 * @param newName
	 *            the new file name
	 */

	public void setFileName( String newName )
	{
		module.setFileName( newName );
		AttributeEvent event = new AttributeEvent( module, AttributeEvent.FILE_NAME_ATTRIBUTE );
		module.broadcastFileNameEvent( event );
	}

	/**
	 * Sets an external file that provides help information for the report.
	 * 
	 * @param helpGuide
	 *            the name of an external file
	 */

	public void setHelpGuide( String helpGuide )
	{
		try
		{
			setStringProperty( HELP_GUIDE_PROP, helpGuide );
		}
		catch ( SemanticException e )
		{
			assert false;
		}
	}

	/**
	 * Sets the script called when the report starts executing.
	 * 
	 * @param value
	 *            the script to set.
	 */

	public void setInitialize( String value )
	{
		try
		{
			setStringProperty( INITIALIZE_METHOD, value );
		}
		catch ( SemanticException e )
		{
			assert false;
		}
	}

	/**
	 * Returns all style element handles that this modules and the included
	 * modules containts.
	 * 
	 * @return all style element handles that this modules and the included
	 *         modules containts.
	 */

	public List getAllStyles( )
	{
		List elementList = module.getModuleNameSpace( Module.STYLE_NAME_SPACE )
				.getElements( );

		return generateHandleList( elementList );
	}

	/**
	 * Returns all data source handles that this modules and the included
	 * modules containts.
	 * 
	 * @return all data source handles that this modules and the included
	 *         modules containts.
	 */

	public List getAllDataSources( )
	{
		List elementList = module.getModuleNameSpace(
				Module.DATA_SOURCE_NAME_SPACE ).getElements( );

		return generateHandleList( elementList );
	}

	/**
	 * Returns all data set handles that this modules and the included modules
	 * containts.
	 * 
	 * @return all data set handles that this modules and the included modules
	 *         containts.
	 */

	public List getAllDataSets( )
	{
		List elementList = module.getModuleNameSpace(
				Module.DATA_SET_NAME_SPACE ).getElements( );

		return generateHandleList( elementList );
	}

	/**
	 * Returns all page handles that this modules and the included modules
	 * containts.
	 * 
	 * @return all page handles that this modules and the included modules
	 *         containts.
	 */

	public List getAllPages( )
	{
		List elementList = module.getModuleNameSpace( Module.PAGE_NAME_SPACE )
				.getElements( );

		return generateHandleList( elementList );
	}

	/**
	 * Returns all parameter handles that this modules and the included modules
	 * containts.
	 * 
	 * @return all parameter handles that this modules and the included modules
	 *         containts.
	 */

	public List getAllParameters( )
	{
		List elementList = module.getModuleNameSpace(
				Module.PARAMETER_NAME_SPACE ).getElements( );

		return generateHandleList( elementList );
	}

	private List generateHandleList( List elementList )
	{
		List handleList = new ArrayList( );

		Iterator iter = elementList.iterator( );
		while ( iter.hasNext( ) )
		{
			DesignElement element = (DesignElement) iter.next( );
			handleList.add( element.getHandle( module ) );
		}
		return handleList;
	}

	/**
	 * Returns the libraries this report design includes. Each in the returned
	 * list is the instance of <code>LibraryHandle</code>.
	 * 
	 * @return the libraries this report design includes.
	 */

	public List getLibraries( )
	{
		if ( module.getLibraries( ) == null )
			return Collections.EMPTY_LIST;

		List libraries = new ArrayList( );

		Iterator iter = module.getLibraries( ).iterator( );
		while ( iter.hasNext( ) )
		{
			Library library = (Library) iter.next( );

			libraries.add( library.handle( ) );
		}
		return libraries;
	}

	/**
	 * Returns the library handle with the given namespace.
	 * 
	 * @param namespace
	 *            the library namespace
	 * @return the library handle with the given namespace
	 */

	public LibraryHandle getLibrary( String namespace )
	{
		Module library = module.getLibraryWithNamespace( namespace );
		if ( library == null )
			return null;

		return (LibraryHandle) library.getHandle( library );
	}

	/**
	 * Shifts the library to new position.
	 * 
	 * @param library
	 *            the library to shift
	 * @param toPosn
	 *            the new position
	 * @throws SemanticException
	 */

	public void shiftLibrary( LibraryHandle library, int toPosn )
			throws SemanticException
	{
		ShiftLibraryCommand command = new ShiftLibraryCommand( module );
		command.shiftLibrary( (Library) library.getElement( ), toPosn );
	}

	/**
	 * Adds one attribute listener. The duplicate listener will not be added.
	 * 
	 * @param listener
	 *            the attribute listener to add
	 */

	public void addAttributeListener( IAttributeListener listener )
	{
		getModule( ).addAttributeListener( listener );
	}

	/**
	 * Removes one attribute listener. If the listener not registered, then the
	 * request is silently ignored.
	 * 
	 * @param listener
	 *            the attribute listener to remove
	 * @return <code>true</code> if <code>listener</code> is successfully
	 *         removed. Otherwise <code>false</code>.
	 *  
	 */

	public boolean removeAttributeListener( IAttributeListener listener )
	{
		return getModule( ).removeAttributeListener( listener );
	}

	/**
	 * Adds one dispose listener. The duplicate listener will not be added.
	 * 
	 * @param listener
	 *            the dispose listener to add
	 */

	public void addDisposeListener( IDisposeListener listener )
	{
		getModule( ).addDisposeListener( listener );
	}

	/**
	 * Removes one dispose listener. If the listener not registered, then the
	 * request is silently ignored.
	 * 
	 * @param listener
	 *            the dispose listener to remove
	 * @return <code>true</code> if <code>listener</code> is successfully
	 *         removed. Otherwise <code>false</code>.
	 *  
	 */

	public boolean removeDisposeListener( IDisposeListener listener )
	{
		return getModule( ).removeDisposeListener( listener );
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.birt.report.model.api.DesignElementHandle#drop()
	 */
	
	public void drop( ) throws SemanticException
	{
		throw new IllegalOperationException( );
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.birt.report.model.api.DesignElementHandle#dropAndClear()
	 */
	
	public void dropAndClear( ) throws SemanticException
	{
		throw new IllegalOperationException( );
	}
}