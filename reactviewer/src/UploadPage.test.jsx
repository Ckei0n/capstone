import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { vi } from 'vitest';
import axios from 'axios';
import UploadPage from './UploadPage';

// Mock axios
vi.mock('axios');
const mockedAxios = vi.mocked(axios);

// render component with router
const renderWithRouter = (component) => {
  return render(
    <BrowserRouter>
      {component}
    </BrowserRouter>
  );
};

describe('UploadPage', () => {
  beforeEach(() => {
    // Reset mocks before each test
    vi.clearAllMocks();
    
    // Mock successful CSRF token fetch for all tests
    mockedAxios.get.mockResolvedValue({
      data: {
        token: 'mock-csrf-token',
        headerName: 'X-CSRF-Token'
      }
    });
  });

  describe('Rendering', () => {
    test('renders all essential elements', async () => {
      renderWithRouter(<UploadPage />);
      
      // Wait for component to fully load after CSRF token fetch
      await waitFor(() => {
        expect(screen.getByText('← Back to Analyzer')).toBeInTheDocument();
      });
      
      expect(screen.getByText('Upload GZ Files')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Upload' })).toBeInTheDocument();
      expect(screen.getByLabelText(/Choose files/i)).toBeInTheDocument();
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
      expect(screen.getByText('0%')).toBeInTheDocument();
    });

    test('renders navigation link with correct path', async () => {
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        const backLink = screen.getByRole('link', { name: '← Back to Analyzer' });
        expect(backLink).toHaveAttribute('href', '/');
      });
    });

    test('file input accepts multiple files', async () => {
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        const fileInput = screen.getByLabelText(/Choose files/i);
        expect(fileInput).toHaveAttribute('multiple');
      });
    });
  });

  describe('CSRF Token Handling', () => {
    test('fetches CSRF token on mount', async () => {
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalledWith('/api/csrf', {
          withCredentials: true
        });
      });
    });

    test('shows error message when CSRF token fetch fails', async () => {
      mockedAxios.get.mockRejectedValueOnce(new Error('Network Error'));
      
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        expect(screen.getByText('Failed to initialize security token')).toBeInTheDocument();
      });
    });

    test('prevents upload when CSRF token is not available', async () => {
      mockedAxios.get.mockRejectedValueOnce(new Error('Token fetch failed'));
      
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        expect(screen.getByText('Failed to initialize security token')).toBeInTheDocument();
      });
      
      const uploadButton = screen.getByRole('button', { name: 'Upload' });
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(screen.getByText('Security token not available. Please refresh the page.')).toBeInTheDocument();
      });
      
      expect(mockedAxios.post).not.toHaveBeenCalled();
    });
  });

  describe('File Selection', () => {
    test('updates state when files are selected', async () => {
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        expect(screen.getByText('← Back to Analyzer')).toBeInTheDocument();
      });
      
      const fileInput = screen.getByLabelText(/Choose files/i);
      const file1 = new File(['content1'], 'test1.gz', { type: 'application/gzip' });
      const file2 = new File(['content2'], 'test2.gz', { type: 'application/gzip' });
      
      fireEvent.change(fileInput, {
        target: { files: [file1, file2] }
      });
      
      expect(fileInput.files).toHaveLength(2);
      expect(fileInput.files[0]).toBe(file1);
      expect(fileInput.files[1]).toBe(file2);
    });
  });

  describe('File Upload - Success Cases', () => {
    test('successful upload shows success message', async () => {
      const mockResponse = { data: 'Files uploaded successfully!' };
      mockedAxios.post.mockResolvedValueOnce(mockResponse);
      
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalled();
      });
      
      const fileInput = screen.getByLabelText(/Choose files/i);
      const uploadButton = screen.getByRole('button', { name: 'Upload' });
      const file = new File(['content'], 'test.gz', { type: 'application/gzip' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(screen.getByText('Files uploaded successfully!')).toBeInTheDocument();
      });
    });

    test('upload progress updates correctly', async () => {
      const mockResponse = { data: 'Upload complete' };
      mockedAxios.post.mockImplementationOnce((url, data, config) => {
        // Simulate progress callback
        if (config.onUploadProgress) {
          config.onUploadProgress({ loaded: 50, total: 100 });
        }
        return Promise.resolve(mockResponse);
      });
      
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalled();
      });
      
      const fileInput = screen.getByLabelText(/Choose files/i);
      const uploadButton = screen.getByRole('button', { name: 'Upload' });
      const file = new File(['content'], 'test.gz', { type: 'application/gzip' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(screen.getByText('50%')).toBeInTheDocument();
      });
    });

    test('sends correct FormData with multiple files', async () => {
      const mockResponse = { data: 'Success' };
      mockedAxios.post.mockResolvedValueOnce(mockResponse);
      
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalled();
      });
      
      const fileInput = screen.getByLabelText(/Choose files/i);
      const uploadButton = screen.getByRole('button', { name: 'Upload' });
      const file1 = new File(['content1'], 'test1.gz', { type: 'application/gzip' });
      const file2 = new File(['content2'], 'test2.gz', { type: 'application/gzip' });
      
      fireEvent.change(fileInput, { target: { files: [file1, file2] } });
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(mockedAxios.post).toHaveBeenCalledWith(
          '/api/import',
          expect.any(FormData),
          expect.objectContaining({
            headers: {
              'Content-Type': 'multipart/form-data',
              'X-CSRF-Token': 'mock-csrf-token'
            },
            withCredentials: true,
            onUploadProgress: expect.any(Function)
          })
        );
      });
    });
  });

  describe('File Upload - Error Cases', () => {
    test('shows error message when upload fails', async () => {
      const mockError = new Error('Network Error');
      mockedAxios.post.mockRejectedValueOnce(mockError);
      
      renderWithRouter(<UploadPage />);
      
  
      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalled();
      });
      
      const fileInput = screen.getByLabelText(/Choose files/i);
      const uploadButton = screen.getByRole('button', { name: 'Upload' });
      const file = new File(['content'], 'test.gz', { type: 'application/gzip' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(screen.getByText('Upload failed: Network Error')).toBeInTheDocument();
      });
    });

    test('handles axios error with response', async () => {
      const mockError = {
        message: 'Request failed with status code 500',
        response: {
          status: 500,
          data: { error: 'Internal Server Error' }
        }
      };
      mockedAxios.post.mockRejectedValueOnce(mockError);
      
      renderWithRouter(<UploadPage />);
      
      
      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalled();
      });
      
      const fileInput = screen.getByLabelText(/Choose files/i);
      const uploadButton = screen.getByRole('button', { name: 'Upload' });
      const file = new File(['content'], 'test.gz', { type: 'application/gzip' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(screen.getByText('Upload failed: Request failed with status code 500')).toBeInTheDocument();
      });
    });
  });

  describe('Edge Cases', () => {
    test('handles upload with no files selected', async () => {
      renderWithRouter(<UploadPage />);
      

      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalled();
      });
      
      const uploadButton = screen.getByRole('button', { name: 'Upload' });
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(mockedAxios.post).toHaveBeenCalledWith(
          '/api/import',
          expect.any(FormData),
          expect.objectContaining({
            headers: {
              'Content-Type': 'multipart/form-data',
              'X-CSRF-Token': 'mock-csrf-token'
            },
            withCredentials: true,
            onUploadProgress: expect.any(Function)
          })
        );
      });
    });

    test('progress bar shows 100% on completion', async () => {
      const mockResponse = { data: 'Complete' };
      mockedAxios.post.mockImplementationOnce((url, data, config) => {
        if (config.onUploadProgress) {
          config.onUploadProgress({ loaded: 100, total: 100 });
        }
        return Promise.resolve(mockResponse);
      });
      
      renderWithRouter(<UploadPage />);
   
      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalled();
      });
      
      const fileInput = screen.getByLabelText(/Choose files/i);
      const uploadButton = screen.getByRole('button', { name: 'Upload' });
      const file = new File(['content'], 'test.gz', { type: 'application/gzip' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(screen.getByText('100%')).toBeInTheDocument();
        expect(screen.getByRole('progressbar')).toHaveAttribute('value', '100');
      });
    });

    test('message state updates correctly on consecutive uploads', async () => {
      renderWithRouter(<UploadPage />);
      
      await waitFor(() => {
        expect(mockedAxios.get).toHaveBeenCalled();
      });
      
      const fileInput = screen.getByLabelText(/Choose files/i);
      const uploadButton = screen.getByRole('button', { name: 'Upload' });
      const file = new File(['content'], 'test.gz', { type: 'application/gzip' });
      
      // First upload - success
      mockedAxios.post.mockResolvedValueOnce({ data: 'First upload success' });
      fireEvent.change(fileInput, { target: { files: [file] } });
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(screen.getByText('First upload success')).toBeInTheDocument();
      });
      
      // Second upload - failure
      mockedAxios.post.mockRejectedValueOnce(new Error('Second upload failed'));
      fireEvent.click(uploadButton);
      
      await waitFor(() => {
        expect(screen.getByText('Upload failed: Second upload failed')).toBeInTheDocument();
        expect(screen.queryByText('First upload success')).not.toBeInTheDocument();
      });
    });
  });
});