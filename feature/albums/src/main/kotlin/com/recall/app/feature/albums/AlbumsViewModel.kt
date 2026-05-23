package com.recall.app.feature.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.database.ExcludedDirectoriesRepository
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.vector.clustering.KMeansClustering
import com.recall.app.core.vector.distance.VectorDistance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val embeddingModel: EmbeddingModel,
    private val vectorIndex: VectorIndex,
    private val mediaItemDao: MediaItemDao,
    private val excludedDirectoriesRepository: ExcludedDirectoriesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private var clusteringJob: Job? = null

    init {
        viewModelScope.launch {
            mediaItemDao.observeIndexedCount()
                .distinctUntilChanged()
                .collect { refresh() }
        }
    }

    fun refresh() {
        clusteringJob?.cancel()
        clusteringJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val albums = withContext(Dispatchers.Default) {
                    buildAlbums()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        albums = albums,
                        error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to generate albums",
                    )
                }
            }
        }
    }

    fun getAlbum(index: Int): Album? = _uiState.value.albums.getOrNull(index)

    private suspend fun buildAlbums(): List<Album> {
        val excludedIds = excludedDirectoriesRepository.getExcludedBucketIds()
        val indexedItems = mediaItemDao.getIndexed()
            .filter { it.bucketId.isEmpty() || it.bucketId !in excludedIds }
        val vectors = indexedItems.mapNotNull { item ->
            vectorIndex.getVector(item.id)?.let { vector -> item.id to vector }
        }
        if (vectors.size < 3) {
            return emptyList()
        }

        val k = KMeansClustering.suggestK(vectors.size)
        val clusters = KMeansClustering.cluster(vectors, k)
        val conceptEmbeddings = CONCEPT_LABELS.associate { (label, text) ->
            label to embeddingModel.embedText(text)
        }

        val rawAlbums = clusters.mapIndexed { index, cluster ->
            val mediaById = indexedItems.associateBy { it.id }
            val memberItems = cluster.memberIds.mapNotNull { mediaById[it] }
            val coverUri = memberItems.firstOrNull()?.uri.orEmpty()
            val name = nameCluster(cluster.centroid, conceptEmbeddings, index)
            Album(
                index = index,
                name = name,
                coverUri = coverUri,
                photoCount = cluster.memberIds.size,
                mediaIds = cluster.memberIds,
            )
        }

        return deduplicateNames(rawAlbums)
            .sortedByDescending { it.photoCount }
            .mapIndexed { sortedIndex, album -> album.copy(index = sortedIndex) }
    }

    private suspend fun nameCluster(
        centroid: FloatArray,
        conceptEmbeddings: Map<String, FloatArray>,
        fallbackIndex: Int,
    ): String {
        var bestLabel: String? = null
        var bestSimilarity = Float.NEGATIVE_INFINITY
        conceptEmbeddings.forEach { (label, embedding) ->
            val similarity = VectorDistance.cosineSimilarity(centroid, embedding)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestLabel = label
            }
        }
        return if (bestSimilarity >= 0.15f && bestLabel != null) {
            bestLabel
        } else {
            "Album ${fallbackIndex + 1}"
        }
    }

    private fun deduplicateNames(albums: List<Album>): List<Album> {
        val nameCounts = mutableMapOf<String, Int>()
        return albums.map { album ->
            val count = nameCounts.getOrDefault(album.name, 0) + 1
            nameCounts[album.name] = count
            if (count == 1) {
                album
            } else {
                album.copy(name = "${album.name} $count")
            }
        }
    }

    companion object {
        private val CONCEPT_LABELS = listOf(
            "Sunsets" to "sunset",
            "Sky & Clouds" to "sky clouds",
            "Ocean & Water" to "ocean water",
            "Nature & Forest" to "forest nature green",
            "Food & Drinks" to "food coffee cake",
            "Night" to "night dark",
            "Snow & Winter" to "snow winter",
            "Flowers" to "flower bloom",
            "Cats" to "cat kitten",
            "Dogs" to "dog puppy",
            "City" to "city building street",
            "Cars & Roads" to "car road vehicle",
            "Beach" to "beach sand tropical",
            "Mountains" to "mountain peak",
            "People" to "person face portrait",
            "Autumn" to "autumn leaves fall",
        )
    }
}
