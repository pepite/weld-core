package org.jboss.weld.environment.tomcat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;

import org.apache.AnnotationProcessor;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
import org.jboss.weld.environment.servlet.util.Reflections;
import org.jboss.weld.manager.api.WeldManager;

/**
 * @author <a href="mailto:matija.mazi@gmail.com">Matija Mazi</a>
 *
 * Forwards all calls in turn to two delegates: first to
 * originalAnnotationProcessor, then to weldProcessor.
 */
public class WeldForwardingAnnotationProcessor extends ForwardingAnnotationProcessor
{
   private final AnnotationProcessor firstProcessor;
   private final AnnotationProcessor secondProcessor;

   public WeldForwardingAnnotationProcessor(AnnotationProcessor originalAnnotationProcessor, AnnotationProcessor weldProcessor)
   {
      this.firstProcessor = originalAnnotationProcessor;
      this.secondProcessor = weldProcessor;
   }

   @Override                   
   protected AnnotationProcessor delegate()
   {
      return firstProcessor;
   }

   @Override
   public void processAnnotations(Object instance) throws IllegalAccessException, InvocationTargetException, NamingException
   {
      super.processAnnotations(instance);
      secondProcessor.processAnnotations(instance);
   }

   @Override
   public void postConstruct(Object instance) throws IllegalAccessException, InvocationTargetException
   {
      super.postConstruct(instance);
      secondProcessor.postConstruct(instance);
   }

   @Override
   public void preDestroy(Object instance) throws IllegalAccessException, InvocationTargetException
   {
      super.preDestroy(instance);
      secondProcessor.preDestroy(instance);
   }

   public static void replaceAnnotationProcessor(ServletContextEvent sce, WeldManager manager)
   {
      StandardContext stdContext = getStandardContext(sce);
      setAnnotationProcessor(stdContext, createInstance(manager, stdContext));
   }

   private static WeldForwardingAnnotationProcessor createInstance(WeldManager manager, StandardContext stdContext)
   {
      try
      {
         Class<?> clazz = Reflections.classForName(WeldAnnotationProcessor.class.getName());
         AnnotationProcessor weldProcessor = (AnnotationProcessor) clazz.getConstructor(WeldManager.class).newInstance(manager);
         return new WeldForwardingAnnotationProcessor(getAnnotationProcessor(stdContext), weldProcessor);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Cannot create WeldForwardingAnnotationProcessor", e);
      }
   }

   private static StandardContext getStandardContext(ServletContextEvent sce)
   {
      try
      {
         // Hack into Tomcat to replace the AnnotationProcessor using reflection to access private fields
         ApplicationContext appContext = (ApplicationContext) getContextFieldValue((ApplicationContextFacade)sce.getServletContext(), ApplicationContextFacade.class);
         return (StandardContext) getContextFieldValue(appContext, ApplicationContext.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Cannot get StandardContext from ServletContext", e);
      }
   }

   private static <E> Object getContextFieldValue(E obj, Class<E> clazz)
           throws NoSuchFieldException, IllegalAccessException
   {
      Field f = clazz.getDeclaredField("context");
      f.setAccessible(true);
      return f.get(obj);
   }

   public static void restoreAnnotationProcessor(ServletContextEvent sce)
   {
      StandardContext stdContext = getStandardContext(sce);
      AnnotationProcessor ap = getAnnotationProcessor(stdContext);
      if (ap instanceof WeldForwardingAnnotationProcessor)
      {
        setAnnotationProcessor(stdContext,((WeldForwardingAnnotationProcessor)ap).firstProcessor);
      }
   }

   private static AnnotationProcessor getAnnotationProcessor(StandardContext stdContext)
   {
      // try the getter first
      try
      {
         Method getter = stdContext.getClass().getDeclaredMethod("getAnnotationProcessor");
         getter.setAccessible(true);
         return (AnnotationProcessor)getter.invoke(stdContext);
      }
      catch (SecurityException e)
      {
         throw new RuntimeException(e);
      }
      catch (IllegalAccessException e)
      {
         throw new RuntimeException(e);
      }
      catch (InvocationTargetException e)
      {
         if (e.getCause() instanceof RuntimeException)
            throw (RuntimeException) e.getCause();
         else
            throw new RuntimeException(e.getCause());
      }
      catch (NoSuchMethodException e)
      {
         // ignore and try the field instead 
      }
      try
      {
         Field field = stdContext.getClass().getDeclaredField("annotationProcessor");
         field.setAccessible(true);
         return (AnnotationProcessor) field.get(stdContext);
      }
      catch (SecurityException e)
      {
         throw new RuntimeException(e);
      }
      catch (IllegalAccessException e)
      {
         throw new RuntimeException(e);
      }
      catch (NoSuchFieldException e)
      {
         // TODO Auto-generated catch block
         throw new RuntimeException("neither field nor setter found for annotationProcessor");
      }
   }
   
   private static void setAnnotationProcessor(StandardContext stdContext, AnnotationProcessor annotationProcessor)
   {
      //try setter first
      try
      {
         Method setter = stdContext.getClass().getDeclaredMethod("setAnnotationProcessor");
         setter.setAccessible(true);
         setter.invoke(stdContext, annotationProcessor);
      }
      catch (SecurityException e)
      {
         throw new RuntimeException(e);
      }
      catch (IllegalAccessException e)
      {
         throw new RuntimeException(e);
      }
      catch (InvocationTargetException e)
      {
         if (e.getCause() instanceof RuntimeException)
            throw (RuntimeException) e.getCause();
         else
            throw new RuntimeException(e.getCause());
      }
      catch (NoSuchMethodException e)
      {
         // ignore and try the method instead
      }
      try
      {
         Field field = stdContext.getClass().getDeclaredField("annotationProcessor");
         field.setAccessible(true);
         field.set(stdContext, annotationProcessor);
      }
      catch (SecurityException e)
      {
         throw new RuntimeException(e);
      }
      catch (IllegalAccessException e)
      {
         throw new RuntimeException(e);
      }
      catch (NoSuchFieldException e)
      {
         throw new RuntimeException("neither field nor setter found for annotationProcessor");
      }
   }
}