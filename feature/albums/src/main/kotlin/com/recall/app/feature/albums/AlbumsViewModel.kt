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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private var cachedConceptEmbeddings: Map<String, FloatArray>? = null

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
        val context = currentCoroutineContext()
        val checkCancellation = { context.ensureActive() }
        checkCancellation()
        embeddingModel.initializationError?.let { error(it) }
        val excludedIds = excludedDirectoriesRepository.getExcludedBucketIds()
        val indexedItems = mediaItemDao.getIndexed()
            .filter { it.bucketId.isEmpty() || it.bucketId !in excludedIds }
        val vectors = indexedItems.mapNotNull { item ->
            checkCancellation()
            vectorIndex.getVector(item.id)?.let { vector -> item.id to vector }
        }
        if (vectors.size < 3) {
            return emptyList()
        }

        val clusters = AlbumGroupingPolicy.withOverlap(
            clusters = KMeansClustering.clusterAutomatically(vectors, checkCancellation = checkCancellation),
            vectors = vectors,
            checkCancellation = checkCancellation,
        )
        val conceptEmbeddings = cachedConceptEmbeddings ?: CONCEPT_LABELS.associate { (label, text) ->
            checkCancellation()
            label to embeddingModel.embedText(text)
        }.also { cachedConceptEmbeddings = it }
        val mediaById = indexedItems.associateBy { it.id }
        val vectorsById = vectors.toMap()

        val rawAlbums = clusters.mapIndexed { index, cluster ->
            checkCancellation()
            val memberItems = cluster.memberIds.mapNotNull { mediaById[it] }
            val coverId = cluster.memberIds.maxByOrNull { id ->
                checkCancellation()
                vectorsById[id]?.let { VectorDistance.cosineSimilarity(it, cluster.centroid) } ?: -1f
            }
            val coverUri = coverId?.let { mediaById[it]?.uri }
                ?: memberItems.firstOrNull()?.uri.orEmpty()
            val name = AlbumGroupingPolicy.name(
                centroid = cluster.centroid,
                memberVectors = cluster.memberIds.mapNotNull { vectorsById[it] },
                conceptEmbeddings = conceptEmbeddings,
                fallbackName = "Album ${index + 1}",
                checkCancellation = checkCancellation,
            )
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
            "Sunsets" to "a photo of a sunset",
            "Sky & Clouds" to "a photo of clouds in the sky",
            "Ocean & Water" to "a photo of the ocean or a lake",
            "Nature & Forest" to "a photo of a forest",
            "Food & Drinks" to "a photo of food or a drink",
            "Night" to "a photo taken at night",
            "Snow & Winter" to "a photo of snow in winter",
            "Flowers" to "a photo of flowers",
            "Cats" to "a photo of a domestic cat",
            "Dogs" to "a photo of a dog",
            "Tigers" to "a photo of a tiger",
            "Lions" to "a photo of a lion",
            "Birds" to "a photo of a bird",
            "Other Animals" to "a photo of a wild animal",
            "City" to "a photo of buildings in a city",
            "Cars & Roads" to "a photo of cars on a road",
            "Beach" to "a photo of a sandy beach",
            "Mountains" to "a photo of mountains",
            "People" to "a photo of people",
            "Autumn" to "a photo of autumn leaves",
            "Screenshots" to "a screenshot of a phone or computer application",
            "Documents" to "a photo of a printed document",
            "Receipts" to "a photo of a shopping receipt",
            "Text & Notes" to "a photo of handwritten notes",
            "Memes" to "an internet meme with text",
            "Art & Illustrations" to "a drawing or illustration",
            "Sports" to "a photo of people playing sports",
            "Interiors" to "a photo of a room indoors",
            "Products" to "a product photo of an object",
        )
    }
}
