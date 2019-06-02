#include <jni.h>
#include <string>
#include <unistd.h> // Used to sleep the thread 5 secons
#include <android/log.h>
#include <chrono>
#include <vector>
#include <thread>
#include <omp.h>
#include "Histogram.h"
#include "RectangularVectors.h"


using namespace std;


void multiplicacionC(float * a1, float * a2, float * a3, jint size) {
    for(int i = 0; i < size; i++){ //Filas
        for(int j = 0; j < size; j++){ //Columnas
            float cij=a3[i*size+j];
            for(int k = 0; k < size; k++){
                cij += a1[i*size+k]*a2[k*size+j];
            }
            a3[i*size+j]=cij;
        }
    }
}

void multiplicacionCthread(int me, int nth, float * a1, float * a2, float * a3, int size) {
    for(int i = (me*size)/nth; i < (me+1)*size/nth; i++){ //Filas
        for(int j = 0; j < size; j++){ //Columnas
            float cij=a3[i*size+j];
            for(int k = 0; k < size; k++){
                a3[i*size+j] += a1[i*size+k]*a2[k*size+j];
            }
            a3[i*size+j]=cij;
        }
    }
}

// NEON Implementation
#if defined(__ARM_ARCH_7A__) && defined(__ARM_NEON__)
#include <arm_neon.h>
void multiplicacionCneon(int me, int nth, float * A, float * B, float * C, int n) {
    for(int i = (me*n)/nth; i < (me+1)*n/nth; i++){ //Filas
        for (int j = 0; j < n; j+=4) {
            float32x4_t c0 = vld1q_f32(C + i * n + j ); /* c0 = C[i][j] */ //vdupq_n_f32(0.0)//c0=0,0,0,0
            for (int k = 0; k < n; k++) {
                c0 = vaddq_f32(c0, /* c0 += A[i][k]*B[k][j] */
                               vmulq_n_f32(vld1q_f32(B + k * n + j),
                                           (float32_t) * (A + i * n + k)));
                vst1q_f32(C + i * n + j, c0); /* C[i][j] = c0 */
            }
        }
    }
}

#define UNROLL (4)
#define BLOCKSIZE 64

void do_block (int n, int si, int sj, int sk,
               float *A, float *B, float *C)
{
    for ( int i = si; i < si+BLOCKSIZE; i+=UNROLL*4 )
        for ( int j = sj; j < sj+BLOCKSIZE; j++ ) {
            float32x4_t c[4];
            for ( int x = 0; x < UNROLL; x++ )
                c[x] = vld1q_f32(C+i+x*4+j*n);
            /* c[x] = C[i][j] */
            for( int k = sk; k < sk+BLOCKSIZE; k++ )
            {
                float32x4_t b = vdupq_n_f32((float32_t) *(B+k+j*n));
                /* b = B[k][j] */
                for (int x = 0; x < UNROLL; x++)
                    c[x] = vaddq_f32(c[x], /* c[x]+=A[i][k]*b */
                                     vmulq_f32(vld1q_f32(A+n*k+x*4+i), b));
            }
            for ( int x = 0; x < UNROLL; x++ )
                vst1q_f32(C+i+x*4+j*n, c[x]);
            /* C[i][j] = c[x] */
        }
}
void square_dgemm (int me, int nth, float* A, float* B, float* C, int n)
{
   for ( int sj = me*n/nth; sj < (me+1)*n/nth; sj += BLOCKSIZE )
        for ( int si = 0; si < n; si += BLOCKSIZE )
            for ( int sk = 0; sk < n; sk += BLOCKSIZE )
                do_block(n, si, sj, sk, A, B, C);
}

void square_dgemmOMP (int nth, float* A, float* B, float* C, int n)
{
    omp_set_num_threads(nth);
#pragma omp parallel for
    for ( int sj = 0; sj < n; sj += BLOCKSIZE )
        for ( int si = 0; si < n; si += BLOCKSIZE )
            for ( int sk = 0; sk < n; sk += BLOCKSIZE )
                do_block(n, si, sj, sk, A, B, C);
}


#endif



////////////////// TAB 2////////////////////
/////////// Single Thread Histogram ////////
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_calcHistogramC(
        JNIEnv *env,
        jobject /* this */,
        jint tam,
        jshortArray imagen_,
        jintArray h_) {

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);
    jboolean isCopyA, isCopyB;
    jlong aux = 0;
    if (imagen == NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {
            jlong aux = 0;
            for (int i = 0; i < 256; i++) {// Inicializa el histograma.
                for (int k = 0; k < 3; k++) h[k + i] = 0;
            }
            for (int i = 0; i < tam; i++) {// Inicializa la imagen.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
                }
            }

            for (int i = 0; i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
                }
            }

            for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) {
                        for (int x = 0; x < 256; x++) {
                            aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] - h[k + x] * h[k + x] * h[k + x] + h[k + x] * h[k + x]);
                            h[k + x] = (int) (aux % 256);
                        }
                        imagen[k + i + j] = (short) (imagen[k + i + j] *h[k + imagen[k + i + j]]);
                    }
                }
            }
        }

        __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
        if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
        if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);
    }

    string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


void histoChunk(int me, int nth, int tam, short* imagen, int* h) {
    long aux = 0;
    for (int i = 0; i < 256; i++) {// Inicializa el histograma.
        for (int k = 0; k < 3; k++) h[k + i] = 0;
    }
    for (int i = 0; i < tam; i++) {// Inicializa la imagen.
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
        }
    }
    for (int i = 0; i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
        }
    }
    for (int i = (me * tam) / nth; i < ((me + 1) * tam) / nth; i++) { // Modificar imagen utilizando el histograma
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) {
                for (int x = 0; x < 256; x++) {
                    aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] -
                                  h[k + x] * h[k + x] * h[k + x] + h[k + x] * h[k + x]);
                    h[k + x] = (int) (aux % 256);
                }
                imagen[k + i + j] = (short) (imagen[k + i + j] * h[k + imagen[k + i + j]]);
            }
        }
    }
}



//////////////////////// TAB 2 ///////////////////////
////////// Multi-Thread Histogram calculation ////////

extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_histogramaCpth(
        JNIEnv *env,
        jobject /* this */,
        jint tam,
        jint nth,
        jshortArray imagen_,
        jintArray h_) {

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);
    int** hpriv;
    jboolean isCopyA, isCopyB;
    vector<thread>t;
    hpriv=new int*[nth];
    for(int i=0; i<nth; i++){
        hpriv[i]=new int[tam];
        for(int j=0; j<tam;j++)hpriv[i][j]=0;
    }
    if (imagen==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {

            for (int i = 0; i < nth; i++)
                t.push_back(thread(histoChunk,i,nth,tam,imagen,hpriv[i]));
            for (int i = 0; i < nth; i++) {
                t[i].join();
                for(int j=0; j<256; j++) h[j]+=hpriv[i][j];
            }
        }
    }
    __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
    if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
    if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);

    std::string terminado = "DONE! Multi-Tread C++";
    return env->NewStringUTF(terminado.c_str());
}


// Multi-Thread using OpenMP
extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_histogramaOpenMP(
        JNIEnv *env,
        jobject /* this */,
        jint tam,
        jshortArray imagen_,
        jintArray h_) {

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);

    jboolean isCopyA, isCopyB;
    if (imagen==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {
            int tid;
            #pragma omp parallel num_threads(8) private(tid)
            {omp_set_num_threads(8);
            tid = omp_get_thread_num();
            printf("Thread = %d\n", tid);

            long aux = 0;
                for (int i = 0; i < 256; i++) {// Inicializa el histograma.
                    for (int k = 0; k < 3; k++) h[k + i] = 0;
                }
                for (int i = 0; i < tam; i++) {// Inicializa la imagen.
                    for (int j = 0; j < tam; j++) {
                        for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
                    }
                }
                #pragma omp critical
                {
                    for (int i = 0;
                         i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
                        for (int j = 0; j < tam; j++) {
                            for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
                        }
                    }
                }
                #pragma omp for nowait
                {
                    for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
                        for (int j = 0; j < tam; j++) {
                            for (int k = 0; k < 3; k++) {
                                for (int x = 0; x < 256; x++) {
                                    aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] -
                                                  h[k + x] * h[k + x] * h[k + x] +
                                                  h[k + x] * h[k + x]);
                                    h[k + x] = (int) (aux % 256);
                                }
                                imagen[k + i + j] = (short) (imagen[k + i + j] *
                                                             h[k + imagen[k + i + j]]);
                            }
                        }
                    }
                }
            }
        }
    }
    __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
    if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
    if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);
    string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

// TAB 2 Single Thread C++ hstigram calculation
extern "C" JNIEXPORT jint JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_histogramCSingleTh( JNIEnv *env,
                                                                           jobject /* this */){
    jint res = 1;
    int tamano = 1000;
    long long aux = 0;
//JAVA TO C++ CONVERTER NOTE: The following call to the 'RectangularVectors' helper class reproduces the rectangular array initialization that is automatic in Java:
//ORIGINAL LINE: int h[][] = new int[3][256];
    std::vector<std::vector<int>> h = RectangularVectors::RectangularIntVector(3, 256); //array con el histograma
    const int tam = 1000; // Imagen de 1.000 x 1.000 pixeles RGB
//JAVA TO C++ CONVERTER NOTE: The following call to the 'RectangularVectors' helper class reproduces the rectangular array initialization that is automatic in Java:
//ORIGINAL LINE: int imagen[][][]= new int[3][tam][tam];
    std::vector<std::vector<std::vector<int>>> imagen = RectangularVectors::RectangularIntVector(3, tam, tam);
    for (int i = 0; i < 256; i++) // Inicializa el histograma.
    {
        for (int k = 0; k < 3; k++)
        {
            h[k][i] = 0;
        }
    }
    for (int i = 0; i < tam; i++) // Inicializa la imagen.
    {
        for (int j = 0; j < tam; j++)
        {
            for (int k = 0; k < 3; k++)
            {
                imagen[k][i][j] = ((i * j) % 256);
            }
        }
    }
    for (int i = 0; i < tam; i++) // Contabiliza el n? veces que aparece cada valor.
    {
        for (int j = 0; j < tam; j++)
        {
            for (int k = 0; k < 3; k++)
            {
                h[k][imagen[k][i][j]]++;
            }
        }
    }

    for (int i = 0; i < tam; i++)
    { // Modificar imagen utilizando el histograma
        for (int j = 0; j < tam; j++)
        {
            for (int k = 0; k < 3; k++)
            {
                for (int x = 0; x < 256; x++)
                {
                    aux = static_cast<long long>(h[k][x] * h[k][x] * h[k][x] * h[k][x] - h[k][x] * h[k][x] * h[k][x] + h[k][x] * h[k][x]);
                    h[k][x] = (aux % 256);
                }
                imagen[k][i][j] = (imagen[k][i][j] * h[k][imagen[k][i][j]]);
            }
        }
    }

    return res;
}



extern "C" JNIEXPORT jint JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_histogramCMultiOpenMP( JNIEnv *env,
                                                                    jobject /* this */){

    jint res = 1;
    int tamano = 1000;
    long long aux = 0;
//JAVA TO C++ CONVERTER NOTE: The following call to the 'RectangularVectors' helper class reproduces the rectangular array initialization that is automatic in Java:
//ORIGINAL LINE: int h[][] = new int[3][256];
    std::vector<std::vector<int>> h = RectangularVectors::RectangularIntVector(3, 256); //array con el histograma
    const int tam = 1000; // Imagen de 1.000 x 1.000 pixeles RGB
//JAVA TO C++ CONVERTER NOTE: The following call to the 'RectangularVectors' helper class reproduces the rectangular array initialization that is automatic in Java:
//ORIGINAL LINE: int imagen[][][]= new int[3][tam][tam];
    std::vector<std::vector<std::vector<int>>> imagen = RectangularVectors::RectangularIntVector(3, tam, tam);

    int tid;
    tid = omp_get_thread_num();
    printf("Thread = %d\n", tid);
    __android_log_print(ANDROID_LOG_DEBUG, "DBC770", "\n this is log messge \n");

    for (int i = 0; i < 256; i++) {// Inicializa el histograma.
        for (int k = 0; k < 3; k++) h[k][i] = 0;
    }
    for (int i = 0; i < tam; i++) {// Inicializa la imagen.
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) imagen[k][i][j] = ((i * j) % 256);
        }
    }

    #pragma omp critical
    {
        for (int i = 0;
             i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
            for (int j = 0; j < tam; j++) {
                for (int k = 0; k < 3; k++) h[k][imagen[k][i][j]]++;
            }
        }
    }

    #pragma omp for nowait
    {
        for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
            for (int j = 0; j < tam; j++) {
                for (int k = 0; k < 3; k++) {
                    for (int x = 0; x < 256; x++) {
                        aux = static_cast<long long>(h[k][x] * h[k][x] * h[k][x] * h[k][x] - h[k][x] * h[k][x] * h[k][x] + h[k][x] * h[k][x]);
                        h[k][x] = (aux % 256);
                    }
                    imagen[k][i][j] = (imagen[k][i][j] * h[k][imagen[k][i][j]]);
                }
            }
        }
    }

    printf("OK");
    return res;
}


extern "C" JNIEXPORT jint JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_histogramCMultiOpenMPBest( JNIEnv *env,
                                                                                    jobject /* this */, jint nThreads){

    jint res = 1;
    int tamano = 1000;
    long long aux = 0;
    jint const nth = nThreads;



//JAVA TO C++ CONVERTER NOTE: The following call to the 'RectangularVectors' helper class reproduces the rectangular array initialization that is automatic in Java:
//ORIGINAL LINE: int h[][] = new int[3][256];
    std::vector<std::vector<int>> h = RectangularVectors::RectangularIntVector(3, 256); //array con el histograma
    const int tam = 1000; // Imagen de 1.000 x 1.000 pixeles RGB
//JAVA TO C++ CONVERTER NOTE: The following call to the 'RectangularVectors' helper class reproduces the rectangular array initialization that is automatic in Java:
//ORIGINAL LINE: int imagen[][][]= new int[3][tam][tam];
    std::vector<std::vector<std::vector<int>>> imagen = RectangularVectors::RectangularIntVector(3, tam, tam);

    int tid;
    #pragma omp parallel num_threads(nth)
    {
        int tid;
        tid = omp_get_thread_num();
        for (int i = 0; i < 256; i++) {// Inicializa el histograma.
            for (int k = 0; k < 3; k++) h[k][i] = 0;
        }
        for (int i = 0; i < tam; i++) {// Inicializa la imagen.
            for (int j = 0; j < tam; j++) {
                for (int k = 0; k < 3; k++) imagen[k][i][j] = ((i * j) % 256);
            }
        }
        #pragma omp for
        {
            for (int i = 0;
                 i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++)  h[k][imagen[k][i][j]]++;
                }
            }
        }
        #pragma omp for nowait
        {
            for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) {
                        for (int x = 0; x < 256; x++) {
                            aux = static_cast<long long>(h[k][x] * h[k][x] * h[k][x] * h[k][x] - h[k][x] * h[k][x] * h[k][x] + h[k][x] * h[k][x]);
                            h[k][x] = (aux % 256);
                        }
                        imagen[k][i][j] = (imagen[k][i][j] * h[k][imagen[k][i][j]]);
                    }
                }
            }
        }
    }

    printf("OK");
    return res;
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentNativo_histogramJArray(
        JNIEnv *env,
        jobject /* this */,
        jint tam,
        jshortArray imagen_,
        jshortArray h_) {

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL); // Gets first dimention [] still needes [][]
    jshort *h = env->GetShortArrayElements(h_, NULL); // Gets first dimention [] still needes [][]

    jlong aux = 0;
    if (imagen == NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {
            jlong aux = 0;
            for (int i = 0; i < 256; i++) {// Inicializa el histograma.
                for (int k = 0; k < 3; k++) h[k + i] = 0;
            }
            for (int i = 0; i < tam; i++) {// Inicializa la imagen.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
                }
            }

            for (int i = 0; i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
                }
            }

            for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) {
                        for (int x = 0; x < 256; x++) {
                            aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] - h[k + x] * h[k + x] * h[k + x] + h[k + x] * h[k + x]);
                            h[k + x] = (int) (aux % 256);
                        }
                        imagen[k + i + j] = (short) (imagen[k + i + j] *h[k + imagen[k + i + j]]);
                    }
                }
            }
        }

        __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
        if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
        if (h!=NULL) env->ReleaseShortArrayElements(h_, h, 0);
    }

    string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}







///////////////////////////////////////////////////////////
////////////// FRAGMENT TAB 3 OTHER METHODS ///////////////

extern "C" JNIEXPORT jfloat JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentOther_tiempoC(
        JNIEnv *env,
        jobject /* this */, jint size) {
    struct timespec t1, t2;
    jfloat aux;
    float *A, *B, *C; //Declaro los arrays
    const int tam=size*size;
    A = (float*)malloc(tam* sizeof(float));
    B = (float*)malloc(tam* sizeof(float));
    C = (float*)malloc(tam* sizeof(float));
    for(int i = 0; i < tam; i++){ //Inicializo los arrays
        A[i] = i*0.0010;
        B[i] = i*0.005;
        C[i] = 0;
    }
    clock_gettime(CLOCK_MONOTONIC, &t1); //Cojo el tiempo antes de la multiplicación
    multiplicacionC(A, B, C, size);
    clock_gettime(CLOCK_MONOTONIC, &t2); //Cojo el tiempo después de la multiplicación
    aux = (jfloat) ((float) (t2.tv_sec - t1.tv_sec) + (t2.tv_nsec - t1.tv_nsec) / (float) 1000000000.0);

    free (A);
    free (B);
    free (C);
    return aux;
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_caceres_bejar_david_histogramakotlin_FragmentOther_tiempoCthread(JNIEnv *env, jobject instance, jint size, jfloatArray jA, jfloatArray jB, jfloatArray jC, jint modo){

    int nth=8;
    float *A, *B, *C; //Declaro los arrays
    const int tam=size*size;
    float tiempo=0;
    jboolean isCopyA, isCopyB, isCopyC;

    A = env->GetFloatArrayElements(jA,&isCopyA); // While arrays of objects must be accessed one entry at a time, arrays of primitives can be read and written directly as if they were declared in C.   http://developer.android.com/training/articles/perf-jni.html
    if (A==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array A reference");
    else
    {
        B = env->GetFloatArrayElements(jB,&isCopyB);
        if (B==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array B reference");
        else{
            C = env->GetFloatArrayElements(jC,&isCopyC);
            if (C==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array C reference");
            else{
                __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "isCopyA is %d, isCopyB is %d, isCopyC is %d",isCopyA, isCopyB, isCopyC);

                if (modo==1) { //Serial implementation
                    auto time1 = chrono::high_resolution_clock::now();
                    multiplicacionC(A, B, C, (int)size);
                    auto time2 = chrono::high_resolution_clock::now();
                    tiempo = chrono::duration<float>(time2-time1).count();
                }
                else if (modo==2) { //Parallel implementation 4 threads
                    vector<thread> t;
                    auto time1 = chrono::high_resolution_clock::now();

                    for (int i = 0; i < nth; i++)
                        t.push_back(thread(multiplicacionCthread, i, nth, A, B, C, (int) size));
                    /* Espera a que terminen los nth threads */
                    for (auto &th: t) th.join();

                    auto time2 = chrono::high_resolution_clock::now();
                    tiempo = chrono::duration<float>(time2-time1).count();
                }
                else if (modo==3) { //Parallel implementation 4 threads + NEON
#if defined(__ARM_ARCH_7A__) && defined(__ARM_NEON__)
                    vector<thread> t;
                    auto time1 = chrono::high_resolution_clock::now();

                    for (int i = 0; i < nth; i++)
                        t.push_back(thread(multiplicacionCneon, i, nth, A, B, C, (int) size));
                    /* Espera a que terminen los nth threads */
                    for (auto &th: t) th.join();

                    auto time2 = chrono::high_resolution_clock::now();
                    tiempo = chrono::duration<float>(time2-time1).count();
#else
                    tiempo=0;
#endif
                }
                else if (modo==4) { //Parallel implementation 4 threads + NEON + tiling
#if defined(__ARM_ARCH_7A__) && defined(__ARM_NEON__)

                    vector<thread> t;
                    auto time1 = chrono::high_resolution_clock::now();
/*
                    for (int i = 0; i < nth; i++)
                        t.push_back(thread(square_dgemm, i, nth, A, B, C, (int) size));
                    // Espera a que terminen los nth threads
                    for (auto &th: t) th.join();
*/
                    square_dgemmOMP(nth,A, B, C, (int) size);
                    auto time2 = chrono::high_resolution_clock::now();
                    tiempo = chrono::duration<float>(time2-time1).count();
#else
                    tiempo=0;
#endif
                }
//                tiempo = (float)chrono::duration<double,milli>(time2-time1).count();
            }
        }
    }
    if (C!=NULL) env->ReleaseFloatArrayElements(jC, C, 0); // care: errors in the type of the pointers are detected at runtime
    if (A!=NULL) env->ReleaseFloatArrayElements(jA, A, 0); // care: errors in the type of the pointers are detected at runtime
    if (B!=NULL) env->ReleaseFloatArrayElements(jB, B, 0); // release must be done even when the original array is not copied into jB
    return tiempo;

}