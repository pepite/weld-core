package org.jboss.weld.annotated.unbacked;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedParameter;

public class UnbackedAnnotatedParameter<X> extends UnbackedAnnotated implements AnnotatedParameter<X>
{

   private final int position;
   private final AnnotatedCallable<X> declaringCallable;
   
   public UnbackedAnnotatedParameter(Type baseType, Set<Type> typeClosure, Set<Annotation> annotations, int position, AnnotatedCallable<X> declaringCallable)
   {
      super(baseType, typeClosure, annotations);
      this.position = position;
      this.declaringCallable = declaringCallable;
   }  

   public int getPosition()
   {
      return position;
   }

   public AnnotatedCallable<X> getDeclaringCallable()
   {
      return declaringCallable;
   }

}
